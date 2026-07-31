package top.harrylei.bitlog.user.service.impl;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.util.StringUtils;
import top.harrylei.bitlog.api.enums.user.UserRoleEnum;
import top.harrylei.bitlog.api.enums.user.UserStatusEnum;
import top.harrylei.bitlog.api.model.auth.LoginParam;
import top.harrylei.bitlog.api.model.auth.PasswordResetParam;
import top.harrylei.bitlog.api.model.auth.RegisterParam;
import top.harrylei.bitlog.api.model.auth.UserCreateParam;
import top.harrylei.bitlog.api.model.user.req.AdminCreateUserParam;
import top.harrylei.bitlog.api.model.user.vo.UserCreatedVO;
import top.harrylei.bitlog.common.config.JwtProperties;
import top.harrylei.bitlog.common.constans.RedisKeyConstants;
import top.harrylei.bitlog.common.context.ReqInfoContext;
import top.harrylei.bitlog.common.enums.ResultCode;
import top.harrylei.bitlog.common.util.MaskUtil;
import top.harrylei.bitlog.common.util.RateLimiter;
import top.harrylei.bitlog.user.component.LoginRateLimiter;
import top.harrylei.bitlog.user.component.VerificationCodeService;
import top.harrylei.bitlog.user.component.VerifyCodePurpose;
import top.harrylei.bitlog.user.repository.dao.UserDAO;
import top.harrylei.bitlog.user.repository.dao.UserInfoDAO;
import top.harrylei.bitlog.user.repository.entity.UserDO;
import top.harrylei.bitlog.user.repository.entity.UserInfoDO;
import top.harrylei.bitlog.user.service.AuthService;
import top.harrylei.bitlog.user.service.LoginResult;
import top.harrylei.bitlog.user.util.JwtUtil;
import top.harrylei.bitlog.user.util.PasswordUtil;

import java.time.Duration;
import java.util.Locale;
import java.util.UUID;
import java.util.concurrent.TimeUnit;

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
    private final JwtUtil jwtUtil;
    private final JwtProperties jwtProperties;
    private final PasswordEncoder passwordEncoder;
    private final StringRedisTemplate redisTemplate;
    private final LoginRateLimiter loginRateLimiter;
    private final RateLimiter rateLimiter;
    private final VerificationCodeService verificationCodeService;

    @Override
    public void sendRegisterCode(String email) {
        String normalizedEmail = normalizeEmail(email);
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

        String normalizedEmail = normalizeEmail(param.getEmail());
        checkAccountAvailable(normalizedEmail, param.getUsername());

        verificationCodeService.verify(VerifyCodePurpose.REGISTER, normalizedEmail, param.getCode());

        doCreateUser(normalizedEmail, param.getUsername(), param.getPassword(), UserRoleEnum.NORMAL);
        log.info("用户注册成功 email={} username={}", MaskUtil.email(normalizedEmail), param.getUsername());
    }

    @Transactional(rollbackFor = Exception.class)
    @Override
    public void createUser(UserCreateParam param) {
        String normalizedEmail = normalizeEmail(param.getEmail());
        checkAccountAvailable(normalizedEmail, param.getUsername());

        if (UserRoleEnum.ADMIN.equals(param.getRole()) && !ReqInfoContext.getContext().isAdmin()) {
            ResultCode.FORBIDDEN.throwException("创建管理员账号需要管理员权限");
        }

        doCreateUser(normalizedEmail, param.getUsername(), param.getPassword(), param.getRole());
        log.info("管理员创建用户成功 email={} username={}", MaskUtil.email(normalizedEmail), param.getUsername());
    }

    @Override
    public LoginResult login(LoginParam param) {
        String normalizedEmail = normalizeEmail(param.getEmail());
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

        Long userId = user.getId();
        UserInfoDO userInfo = userInfoDAO.getByUserId(userId);
        UserRoleEnum role = userInfo != null ? userInfo.getUserRole() : UserRoleEnum.NORMAL;

        return issueTokenPair(userId, role);
    }

    @Override
    public LoginResult refresh(String refreshToken) {
        String redisKey = RedisKeyConstants.getUserRefreshTokenKey(refreshToken);
        String storedValue = redisTemplate.opsForValue().get(redisKey);
        if (storedValue == null) {
            ResultCode.REFRESH_TOKEN_INVALID.throwException();
        }

        String[] parts = storedValue.split(":", 2);
        Long userId = Long.parseLong(parts[0]);
        UserRoleEnum role = UserRoleEnum.valueOf(parts[1]);

        redisTemplate.delete(redisKey);

        UserDO user = userDAO.getById(userId);
        if (user == null || !UserStatusEnum.ENABLED.equals(user.getStatus())) {
            ResultCode.USER_DISABLED.throwException();
        }

        LoginResult result = issueTokenPair(userId, role);

        log.info("用户刷新 Token 成功 userId={}", userId);
        return result;
    }

    @Override
    public void logout(String refreshToken) {
        if (!StringUtils.hasText(refreshToken)) {
            return;
        }
        try {
            redisTemplate.delete(RedisKeyConstants.getUserRefreshTokenKey(refreshToken));
            log.info("用户退出登录，Refresh Token 已撤销");
        } catch (Exception e) {
            log.error("退出登录删除 Refresh Token 异常", e);
        }
    }

    @Override
    public void sendResetPasswordCode(String email) {
        String normalizedEmail = normalizeEmail(email);
        verificationCodeService.guardSendIp();

        if (userDAO.getByEmail(normalizedEmail) == null) {
            return;
        }

        verificationCodeService.issueAndSend(VerifyCodePurpose.RESET_PASSWORD, normalizedEmail);
    }

    @Transactional(rollbackFor = Exception.class)
    @Override
    public void resetPassword(PasswordResetParam param) {
        String normalizedEmail = normalizeEmail(param.getEmail());
        verificationCodeService.verify(VerifyCodePurpose.RESET_PASSWORD, normalizedEmail, param.getCode());

        UserDO user = userDAO.getByEmail(normalizedEmail);
        if (user == null) {
            ResultCode.USER_NOT_EXISTS.throwException();
        }

        userDAO.updatePassword(user.getId(), passwordEncoder.encode(param.getNewPassword()));
        log.info("密码重置成功 userId={}", user.getId());
    }

    @Transactional(rollbackFor = Exception.class)
    @Override
    public UserCreatedVO adminCreateUser(AdminCreateUserParam req) {
        String normalizedEmail = normalizeEmail(req.getEmail());
        if (userDAO.isUsernameTaken(req.getUsername())) {
            ResultCode.USER_ALREADY_EXISTS.throwException(req.getUsername());
        }
        if (normalizedEmail != null && userDAO.isEmailTaken(normalizedEmail)) {
            ResultCode.USER_ALREADY_EXISTS.throwException(normalizedEmail);
        }

        String password = PasswordUtil.generateRandomPassword();
        Long userId = doCreateUser(normalizedEmail, req.getUsername(), password, req.getUserRole());
        userInfoDAO.updateInfo(userId, req.getProfile(), req.getPosition(), req.getCompany());

        log.info("管理员创建用户成功 username={}", req.getUsername());
        return new UserCreatedVO().setUsername(req.getUsername()).setInitialPassword(password);
    }

    private void checkAccountAvailable(String email, String username) {
        if (userDAO.isEmailTaken(email)) {
            ResultCode.USER_ALREADY_EXISTS.throwException(email);
        }
        if (userDAO.isUsernameTaken(username)) {
            ResultCode.USER_ALREADY_EXISTS.throwException(username);
        }
    }

    private Long doCreateUser(String email, String username, String rawPassword, UserRoleEnum role) {
        UserDO newUser =
            new UserDO().setUsername(username).setEmail(email).setPassword(passwordEncoder.encode(rawPassword));
        userDAO.save(newUser);

        UserInfoDO userInfo = new UserInfoDO().setUserId(newUser.getId()).setAvatar("").setUserRole(role);
        userInfoDAO.save(userInfo);
        return newUser.getId();
    }

    private String normalizeEmail(String email) {
        return StringUtils.hasText(email) ? email.trim().toLowerCase(Locale.ROOT) : null;
    }

    private LoginResult issueTokenPair(Long userId, UserRoleEnum role) {
        String accessToken = jwtUtil.generateToken(userId, role);
        String refreshToken = UUID.randomUUID().toString();
        String redisValue = userId + ":" + role.name();

        redisTemplate.opsForValue().set(RedisKeyConstants.getUserRefreshTokenKey(refreshToken), redisValue,
            jwtProperties.getRefreshTokenExpire().getSeconds(), TimeUnit.SECONDS);

        log.info("颁发 Token 对 userId={}", userId);
        return new LoginResult(accessToken, refreshToken);
    }
}
