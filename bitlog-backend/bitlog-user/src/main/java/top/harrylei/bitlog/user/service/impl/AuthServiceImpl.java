package top.harrylei.bitlog.user.service.impl;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.context.ApplicationEventPublisher;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.util.StringUtils;
import top.harrylei.bitlog.api.enums.user.UserRoleEnum;
import top.harrylei.bitlog.api.enums.user.UserStatusEnum;
import top.harrylei.bitlog.api.model.auth.LoginParam;
import top.harrylei.bitlog.api.model.auth.OAuthLoginParam;
import top.harrylei.bitlog.api.model.auth.PasswordResetParam;
import top.harrylei.bitlog.api.model.auth.RegisterParam;
import top.harrylei.bitlog.api.model.user.UserRules;
import top.harrylei.bitlog.api.model.user.req.AdminCreateUserParam;
import top.harrylei.bitlog.api.model.user.vo.UserCreatedVO;
import top.harrylei.bitlog.common.constans.RedisKeyConstants;
import top.harrylei.bitlog.common.context.ReqInfoContext;
import top.harrylei.bitlog.common.enums.ResultCode;
import top.harrylei.bitlog.common.util.EmailUtil;
import top.harrylei.bitlog.common.util.MaskUtil;
import top.harrylei.bitlog.common.util.RateLimiter;
import top.harrylei.bitlog.user.component.LoginRateLimiter;
import top.harrylei.bitlog.user.component.OAuthAvatarEvent;
import top.harrylei.bitlog.user.component.RefreshTokenStore;
import top.harrylei.bitlog.user.component.UsernameGenerator;
import top.harrylei.bitlog.user.component.VerificationCodeService;
import top.harrylei.bitlog.user.component.VerifyCodePurpose;
import top.harrylei.bitlog.user.repository.dao.UserDAO;
import top.harrylei.bitlog.user.repository.dao.UserInfoDAO;
import top.harrylei.bitlog.user.repository.dao.UserIdentityDAO;
import top.harrylei.bitlog.user.repository.entity.UserDO;
import top.harrylei.bitlog.user.repository.entity.UserInfoDO;
import top.harrylei.bitlog.user.repository.entity.UserIdentityDO;
import top.harrylei.bitlog.user.service.AuthService;
import top.harrylei.bitlog.user.service.LoginResult;
import top.harrylei.bitlog.user.util.JwtUtil;
import top.harrylei.bitlog.user.util.PasswordUtil;

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

    private final UserDAO userDAO;
    private final UserInfoDAO userInfoDAO;
    private final UserIdentityDAO userIdentityDAO;
    private final UsernameGenerator usernameGenerator;
    private final ApplicationEventPublisher eventPublisher;
    private final JwtUtil jwtUtil;
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

        if (userDAO.isEmailTaken(normalizedEmail)) {
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
        checkAccountAvailable(normalizedEmail, param.getUsername());

        verificationCodeService.verify(VerifyCodePurpose.REGISTER, normalizedEmail, param.getCode());

        doCreateUser(normalizedEmail, param.getUsername(), param.getPassword(), UserRoleEnum.NORMAL);
        log.info("用户注册成功 email={} username={}", MaskUtil.email(normalizedEmail), param.getUsername());
    }

    @Override
    public LoginResult login(LoginParam param) {
        String normalizedEmail = EmailUtil.normalize(param.getEmail());
        String clientIp = ReqInfoContext.getContext().getClientIp();
        loginRateLimiter.check(clientIp, normalizedEmail);

        UserDO user = userDAO.getByEmail(normalizedEmail);
        if (user == null || !passwordEncoder.matches(param.getPassword(), user.getPassword())) {
            loginRateLimiter.increment(clientIp, normalizedEmail);
            ResultCode.ACCOUNT_OR_PASSWORD_ERROR.throwException();
        }

        if (!UserStatusEnum.ENABLED.equals(user.getStatus())) {
            ResultCode.USER_DISABLED.throwException(normalizedEmail);
        }

        loginRateLimiter.reset(clientIp, normalizedEmail);

        return issueTokenPair(user.getId(), user.getUserRole());
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
        UserDO existing = email != null ? userDAO.getByEmail(email) : null;
        if (existing != null) {
            // 该账号可能已绑同平台的另一个号（绑定不要求两侧邮箱一致），再并入会撞 uk_user_provider。
            // 拦在这里是为了给出可读原因，否则用户只会看到回跳后的 server_error
            if (userIdentityDAO.existsByUserAndProvider(existing.getId(), param.getProvider())) {
                ResultCode.OPERATION_NOT_ALLOWED.throwException("该邮箱对应的账号已绑定其他账号，请用原账号登录后处理");
            }
            userIdentityDAO.bind(existing.getId(), param.getProvider(), param.getProviderUserId(), email);
            log.info("第三方身份并入既有账号 userId={} provider={}", existing.getId(), param.getProvider());
            return issueTokenForUser(existing.getId());
        }

        String username = usernameGenerator.generate(param.getName(), email);
        Long userId = doCreateUser(email, username, null, UserRoleEnum.NORMAL);
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
        UserDO user = userDAO.getById(userId);
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

        if (userDAO.getByEmail(normalizedEmail) == null) {
            return;
        }

        verificationCodeService.issueAndSend(VerifyCodePurpose.RESET_PASSWORD, normalizedEmail);
    }

    @Transactional(rollbackFor = Exception.class)
    @Override
    public void resetPassword(PasswordResetParam param) {
        String normalizedEmail = EmailUtil.normalize(param.getEmail());
        verificationCodeService.verify(VerifyCodePurpose.RESET_PASSWORD, normalizedEmail, param.getCode());

        UserDO user = userDAO.getByEmail(normalizedEmail);
        if (user == null) {
            ResultCode.USER_NOT_EXISTS.throwException();
        }

        userDAO.updatePassword(user.getId(), passwordEncoder.encode(param.getNewPassword()));
        // 忘记密码走的是邮箱验证码，请求不带登录态，发起者所在设备上也没有会话可保留，全部撤销
        refreshTokenStore.revokeAll(user.getId(), null);
        log.info("密码重置成功 userId={}", user.getId());
    }

    @Transactional(rollbackFor = Exception.class)
    @Override
    public UserCreatedVO adminCreateUser(AdminCreateUserParam req) {
        String normalizedEmail = EmailUtil.normalize(req.getEmail());
        checkAccountAvailable(normalizedEmail, req.getUsername());

        String password = PasswordUtil.generateRandomPassword();
        Long userId = doCreateUser(normalizedEmail, req.getUsername(), password, req.getUserRole());
        userInfoDAO.updateInfo(userId, req.getProfile(), req.getPosition(), req.getCompany());

        log.info("管理员创建用户成功 email={} username={}", MaskUtil.email(normalizedEmail), req.getUsername());
        return new UserCreatedVO().setEmail(normalizedEmail).setUsername(req.getUsername())
            .setInitialPassword(password);
    }

    private void checkAccountAvailable(String email, String username) {
        if (userDAO.isEmailTaken(email)) {
            ResultCode.USER_ALREADY_EXISTS.throwException(email);
        }
        if (UserRules.isReserved(username) || userDAO.isUsernameTaken(username)) {
            ResultCode.USER_ALREADY_EXISTS.throwException(username);
        }
    }

    private Long doCreateUser(String email, String username, String rawPassword, UserRoleEnum role) {
        // 第三方登录建号时没有密码，留空即可：Bcrypt 对空密文一律返回不匹配，密码登录自然走不通
        String encodedPassword = rawPassword != null ? passwordEncoder.encode(rawPassword) : null;
        UserDO newUser =
            new UserDO().setUsername(username).setEmail(email).setPassword(encodedPassword).setUserRole(role);
        userDAO.save(newUser);

        UserInfoDO userInfo = new UserInfoDO().setUserId(newUser.getId()).setAvatar("");
        userInfoDAO.save(userInfo);
        return newUser.getId();
    }

    private LoginResult issueTokenForUser(Long userId) {
        UserDO user = userDAO.getById(userId);
        if (user == null || !UserStatusEnum.ENABLED.equals(user.getStatus())) {
            ResultCode.USER_DISABLED.throwException();
        }
        return issueTokenPair(userId, user.getUserRole());
    }

    private LoginResult issueTokenPair(Long userId, UserRoleEnum role) {
        String accessToken = jwtUtil.generateToken(userId, role);
        String refreshToken = refreshTokenStore.issue(userId, role);

        log.info("颁发 Token 对 userId={}", userId);
        return new LoginResult(accessToken, refreshToken);
    }
}
