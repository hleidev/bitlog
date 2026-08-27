package top.harrylei.bitlog.auth.service.impl;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.context.ApplicationEventPublisher;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.util.StringUtils;
import top.harrylei.bitlog.user.model.enums.UserRoleEnum;
import top.harrylei.bitlog.user.model.enums.UserStatusEnum;
import top.harrylei.bitlog.auth.model.LoginParam;
import top.harrylei.bitlog.auth.model.OAuthLoginParam;
import top.harrylei.bitlog.auth.model.PasswordResetParam;
import top.harrylei.bitlog.auth.model.RegisterParam;
import top.harrylei.bitlog.user.model.dto.UserAccountDTO;
import top.harrylei.bitlog.auth.model.AdminCreateUserParam;
import top.harrylei.bitlog.auth.model.UserCreatedVO;
import top.harrylei.bitlog.common.constants.RedisKeyConstants;
import top.harrylei.bitlog.common.context.ReqInfoContext;
import top.harrylei.bitlog.common.enums.ResultCode;
import top.harrylei.bitlog.common.util.EmailUtil;
import top.harrylei.bitlog.common.util.MaskUtil;
import top.harrylei.bitlog.common.util.RateLimiter;
import top.harrylei.bitlog.auth.security.LoginRateLimiter;
import top.harrylei.bitlog.user.event.OAuthAvatarEvent;
import top.harrylei.bitlog.auth.support.RefreshTokenStore;
import top.harrylei.bitlog.auth.service.VerificationCodeService;
import top.harrylei.bitlog.auth.service.VerifyCodePurpose;
import top.harrylei.bitlog.auth.repository.dao.UserIdentityDAO;
import top.harrylei.bitlog.auth.repository.entity.UserIdentityDO;
import top.harrylei.bitlog.auth.service.AuthService;
import top.harrylei.bitlog.auth.service.LoginResult;
import top.harrylei.bitlog.user.service.UserAccountService;
import top.harrylei.bitlog.auth.security.JwtTokenIssuer;
import top.harrylei.bitlog.auth.util.PasswordUtil;

import java.time.Duration;

/**
 * 认证服务实现
 *
 * @author Harry
 * @since 2026-03-20
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class AuthServiceImpl implements AuthService {

    private static final Duration REGISTER_WINDOW = Duration.ofHours(1);
    private static final int REGISTER_MAX = 10;

    private final UserAccountService userAccountService;
    private final UserIdentityDAO userIdentityDAO;
    private final ApplicationEventPublisher eventPublisher;
    private final JwtTokenIssuer jwtTokenIssuer;
    private final RefreshTokenStore refreshTokenStore;
    private final PasswordEncoder passwordEncoder;
    private final StringRedisTemplate redisTemplate;
    private final LoginRateLimiter loginRateLimiter;
    private final RateLimiter rateLimiter;
    private final VerificationCodeService verificationCodeService;

    @Override
    public void sendRegisterCode(String email) {
        String normalizedEmail = EmailUtil.normalize(email);
        verificationCodeService.guardSendIp();

        if (userAccountService.isEmailTaken(normalizedEmail)) {
            ResultCode.USER_ALREADY_EXISTS.throwException(normalizedEmail);
        }

        verificationCodeService.issueAndSend(VerifyCodePurpose.REGISTER, normalizedEmail);
    }

    @Transactional(rollbackFor = Exception.class)
    @Override
    public void register(RegisterParam param) {
        String clientIp = ReqInfoContext.getContext().getClientIp();
        if (clientIp != null) {
            rateLimiter.acquireOrThrow(RedisKeyConstants.getRegisterIpKey(clientIp), REGISTER_MAX, REGISTER_WINDOW);
        }

        String normalizedEmail = EmailUtil.normalize(param.getEmail());
        userAccountService.checkAccountAvailable(normalizedEmail, param.getUsername());

        verificationCodeService.verify(VerifyCodePurpose.REGISTER, normalizedEmail, param.getCode());

        encodeAndCreateAccount(normalizedEmail, param.getUsername(), param.getPassword(), UserRoleEnum.NORMAL);
        log.info("用户注册成功 email={} username={}", MaskUtil.email(normalizedEmail), param.getUsername());
    }

    @Override
    public LoginResult login(LoginParam param) {
        String normalizedEmail = EmailUtil.normalize(param.getEmail());
        String clientIp = ReqInfoContext.getContext().getClientIp();
        loginRateLimiter.check(clientIp, normalizedEmail);

        UserAccountDTO user = userAccountService.getByEmail(normalizedEmail);
        if (user == null || !passwordEncoder.matches(param.getPassword(), user.getPassword())) {
            loginRateLimiter.increment(clientIp, normalizedEmail);
            ResultCode.ACCOUNT_OR_PASSWORD_ERROR.throwException();
        }

        if (!UserStatusEnum.ENABLED.equals(user.getStatus())) {
            ResultCode.USER_DISABLED.throwException(normalizedEmail);
        }

        loginRateLimiter.reset(clientIp, normalizedEmail);

        return issueTokenPair(user.getUserId(), user.getUserRole());
    }

    @Transactional(rollbackFor = Exception.class)
    @Override
    public LoginResult loginWithOAuth(OAuthLoginParam param) {
        UserIdentityDO identity = userIdentityDAO.getByProvider(param.getProvider(), param.getProviderUserId());
        if (identity != null) {
            return issueTokenForUser(identity.getUserId());
        }

        // 邮箱是下面合并账号与建号的唯一依据，未经平台验证就用它等于让任何人
        // 注册一个填着他人邮箱的第三方账号，即可登进对方账号
        if (!param.isEmailVerified()) {
            ResultCode.OAUTH_EMAIL_UNVERIFIED.throwException();
        }

        // 同一个人先用邮箱注册、后用第三方登录，按邮箱并入既有账号，避免产生两个孤立账号
        String email = EmailUtil.normalize(param.getEmail());
        UserAccountDTO existing = email != null ? userAccountService.getByEmail(email) : null;
        if (existing != null) {
            // 该账号可能已绑同平台的另一个号（绑定不要求两侧邮箱一致），再并入会撞 uk_user_provider。
            // 拦在这里是为了给出可读原因，否则用户只会看到回跳后的 server_error
            if (userIdentityDAO.existsByUserAndProvider(existing.getUserId(), param.getProvider())) {
                ResultCode.OPERATION_NOT_ALLOWED.throwException("该邮箱对应的账号已绑定其他账号，请用原账号登录后处理");
            }
            userIdentityDAO.bind(existing.getUserId(), param.getProvider(), param.getProviderUserId(), email);
            log.info("第三方身份并入既有账号 userId={} provider={}", existing.getUserId(), param.getProvider());
            return issueTokenForUser(existing.getUserId());
        }

        String username = userAccountService.generateUsername(param.getName(), email);
        Long userId = encodeAndCreateAccount(email, username, null, UserRoleEnum.NORMAL);
        userIdentityDAO.bind(userId, param.getProvider(), param.getProviderUserId(), email);

        // 头像转存要读到刚建的这行记录，交由事务提交后的监听器异步处理
        eventPublisher.publishEvent(new OAuthAvatarEvent(userId, param.getAvatarUrl()));

        log.info("第三方登录首次建号 userId={} provider={} username={}", userId, param.getProvider(), username);
        return issueTokenForUser(userId);
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public void bindWithOAuth(String intentToken, OAuthLoginParam param) {
        // 令牌一次性消费：授权链路可能被重放，取完即删使重放无从关联到账号
        String userIdValue =
            redisTemplate.opsForValue().getAndDelete(RedisKeyConstants.getOAuthBindIntentKey(intentToken));
        if (userIdValue == null) {
            throw ResultCode.INVALID_PARAMETER.toException("绑定请求已失效，请重新发起");
        }
        Long userId = Long.valueOf(userIdValue);

        // 绑定不校验 emailVerified：认的是平台侧唯一标识，邮箱在此只作展示
        UserIdentityDO existing = userIdentityDAO.getByProvider(param.getProvider(), param.getProviderUserId());
        if (existing != null) {
            if (existing.getUserId().equals(userId)) {
                return;
            }
            // 放行会让一个第三方账号能登进两个本站账号，登录时无从判断该进哪个
            throw ResultCode.OPERATION_NOT_ALLOWED.toException("该账号已绑定到其他用户");
        }
        if (userIdentityDAO.existsByUserAndProvider(userId, param.getProvider())) {
            throw ResultCode.OPERATION_NOT_ALLOWED.toException("已绑定该平台账号，请先解绑");
        }

        userIdentityDAO.bind(userId, param.getProvider(), param.getProviderUserId(),
            EmailUtil.normalize(param.getEmail()));
        log.info("绑定第三方身份 userId={} provider={}", userId, param.getProvider());
    }

    @Override
    public LoginResult refresh(String refreshToken) {
        RefreshTokenStore.Payload payload = refreshTokenStore.consume(refreshToken);
        if (payload == null) {
            ResultCode.REFRESH_TOKEN_INVALID.throwException();
        }

        Long userId = payload.userId();
        UserAccountDTO user = userAccountService.getById(userId);
        if (user == null || !UserStatusEnum.ENABLED.equals(user.getStatus())) {
            ResultCode.USER_DISABLED.throwException();
        }

        LoginResult result = issueTokenPair(userId, payload.role());

        log.info("用户刷新 Token 成功 userId={}", userId);
        return result;
    }

    @Override
    public void logout(String refreshToken) {
        if (!StringUtils.hasText(refreshToken)) {
            return;
        }
        try {
            refreshTokenStore.revoke(refreshToken);
            log.info("用户退出登录，Refresh Token 已撤销");
        } catch (Exception e) {
            log.error("退出登录删除 Refresh Token 异常", e);
        }
    }

    @Override
    public void sendResetPasswordCode(String email) {
        String normalizedEmail = EmailUtil.normalize(email);
        verificationCodeService.guardSendIp();

        if (userAccountService.getByEmail(normalizedEmail) == null) {
            return;
        }

        verificationCodeService.issueAndSend(VerifyCodePurpose.RESET_PASSWORD, normalizedEmail);
    }

    @Transactional(rollbackFor = Exception.class)
    @Override
    public void resetPassword(PasswordResetParam param) {
        String normalizedEmail = EmailUtil.normalize(param.getEmail());
        verificationCodeService.verify(VerifyCodePurpose.RESET_PASSWORD, normalizedEmail, param.getCode());

        UserAccountDTO user = userAccountService.getByEmail(normalizedEmail);
        if (user == null) {
            ResultCode.USER_NOT_EXISTS.throwException();
        }

        userAccountService.updatePassword(user.getUserId(), passwordEncoder.encode(param.getNewPassword()));
        // 忘记密码走的是邮箱验证码，请求不带登录态，发起者所在设备上也没有会话可保留，全部撤销
        refreshTokenStore.revokeAll(user.getUserId(), null);
        log.info("密码重置成功 userId={}", user.getUserId());
    }

    @Transactional(rollbackFor = Exception.class)
    @Override
    public UserCreatedVO adminCreateUser(AdminCreateUserParam req) {
        String normalizedEmail = EmailUtil.normalize(req.getEmail());
        userAccountService.checkAccountAvailable(normalizedEmail, req.getUsername());

        String password = PasswordUtil.generateRandomPassword();
        Long userId = encodeAndCreateAccount(normalizedEmail, req.getUsername(), password, req.getUserRole());
        userAccountService.updateProfile(userId, req.getProfile(), req.getPosition(), req.getCompany());

        log.info("管理员创建用户成功 email={} username={}", MaskUtil.email(normalizedEmail), req.getUsername());
        return new UserCreatedVO().setEmail(normalizedEmail).setUsername(req.getUsername())
            .setInitialPassword(password);
    }

    private Long encodeAndCreateAccount(String email, String username, String rawPassword, UserRoleEnum role) {
        // 第三方登录建号时没有密码，留空即可：Bcrypt 对空密文一律返回不匹配，密码登录自然走不通
        String encodedPassword = rawPassword != null ? passwordEncoder.encode(rawPassword) : null;
        return userAccountService.createAccount(email, username, encodedPassword, role);
    }

    private LoginResult issueTokenForUser(Long userId) {
        UserAccountDTO user = userAccountService.getById(userId);
        if (user == null || !UserStatusEnum.ENABLED.equals(user.getStatus())) {
            ResultCode.USER_DISABLED.throwException();
        }
        return issueTokenPair(userId, user.getUserRole());
    }

    private LoginResult issueTokenPair(Long userId, UserRoleEnum role) {
        String accessToken = jwtTokenIssuer.generateToken(userId, role);
        String refreshToken = refreshTokenStore.issue(userId, role);

        log.info("颁发 Token 对 userId={}", userId);
        return new LoginResult(accessToken, refreshToken);
    }
}
