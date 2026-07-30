package top.harrylei.bitlog.user.service.impl;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.util.StringUtils;
import top.harrylei.bitlog.api.enums.user.LoginTypeEnum;
import top.harrylei.bitlog.api.enums.user.UserRoleEnum;
import top.harrylei.bitlog.api.enums.user.UserStatusEnum;
import top.harrylei.bitlog.api.model.user.req.AdminCreateUserParam;
import top.harrylei.bitlog.api.model.user.vo.UserCreatedVO;
import top.harrylei.bitlog.common.constans.RedisKeyConstants;
import top.harrylei.bitlog.common.context.ReqInfoContext;
import top.harrylei.bitlog.common.enums.ResultCode;
import top.harrylei.bitlog.common.exception.BusinessException;
import top.harrylei.bitlog.common.mail.MailService;
import top.harrylei.bitlog.common.util.RateLimiter;
import top.harrylei.bitlog.common.config.JwtProperties;
import top.harrylei.bitlog.user.component.LoginRateLimiter;
import top.harrylei.bitlog.user.repository.dao.UserDAO;
import top.harrylei.bitlog.user.repository.dao.UserInfoDAO;
import top.harrylei.bitlog.user.repository.entity.UserDO;
import top.harrylei.bitlog.user.repository.entity.UserInfoDO;
import top.harrylei.bitlog.user.service.AuthService;
import top.harrylei.bitlog.user.service.LoginResult;
import top.harrylei.bitlog.user.util.JwtUtil;
import top.harrylei.bitlog.user.util.PasswordUtil;

import java.security.SecureRandom;
import java.time.Duration;
import java.util.List;
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

    private static final Duration CODE_TTL = Duration.ofMinutes(5);
    private static final int CODE_MAX_ATTEMPTS = 5;
    private static final int CODE_ORIGIN = 100000;
    private static final int CODE_BOUND = 1000000;
    private static final Duration MAIL_COOLDOWN = Duration.ofSeconds(60);
    private static final Duration MAIL_DAILY_WINDOW = Duration.ofHours(24);
    private static final int MAIL_DAILY_MAX = 5;
    private static final Duration MAIL_IP_WINDOW = Duration.ofHours(1);
    private static final int MAIL_IP_MAX = 20;
    private static final Duration REGISTER_WINDOW = Duration.ofHours(1);
    private static final int REGISTER_MAX = 10;
    private static final String PURPOSE_REGISTER = "register";
    private static final String PURPOSE_RESET = "reset";

    private static final SecureRandom SECURE_RANDOM = new SecureRandom();

    private final UserDAO userDAO;
    private final UserInfoDAO userInfoDAO;
    private final JwtUtil jwtUtil;
    private final JwtProperties jwtProperties;
    private final PasswordEncoder passwordEncoder;
    private final StringRedisTemplate redisTemplate;
    private final LoginRateLimiter loginRateLimiter;
    private final RateLimiter rateLimiter;
    private final MailService mailService;

    @Override
    public void sendRegisterCode(String email) {
        String normalizedEmail = normalizeEmail(email);
        guardMailIp();
        if (userDAO.existsEmail(normalizedEmail)) {
            ResultCode.USER_ALREADY_EXISTS.throwException(normalizedEmail);
        }
        guardMailSend(PURPOSE_REGISTER, normalizedEmail);

        String code = issueCode(RedisKeyConstants.getRegisterCodeKey(normalizedEmail));
        mailService.send(normalizedEmail, "注册验证码", codeHtml("完成注册", code));
    }

    @Transactional(rollbackFor = Exception.class)
    @Override
    public void register(String email, String username, String password, String code) {
        String clientIp = ReqInfoContext.getContext().getClientIp();
        if (clientIp != null) {
            guard(RedisKeyConstants.getRegisterIpKey(clientIp), REGISTER_MAX, REGISTER_WINDOW);
        }

        String normalizedEmail = normalizeEmail(email);
        if (userDAO.existsEmail(normalizedEmail)) {
            ResultCode.USER_ALREADY_EXISTS.throwException(normalizedEmail);
        }
        if (userDAO.existsUser(username)) {
            ResultCode.USER_ALREADY_EXISTS.throwException(username);
        }

        verifyCode(RedisKeyConstants.getRegisterCodeKey(normalizedEmail),
            RedisKeyConstants.getRegisterCodeAttemptsKey(normalizedEmail), code);

        doCreateUser(normalizedEmail, username, password, UserRoleEnum.NORMAL, true, null, null, null);
        log.info("用户注册成功 email={} username={}", normalizedEmail, username);
    }

    @Transactional(rollbackFor = Exception.class)
    @Override
    public void createUser(String email, String username, String password, UserRoleEnum userRole) {
        String normalizedEmail = normalizeEmail(email);
        if (userDAO.existsEmail(normalizedEmail)) {
            ResultCode.USER_ALREADY_EXISTS.throwException(normalizedEmail);
        }
        if (userDAO.existsUser(username)) {
            ResultCode.USER_ALREADY_EXISTS.throwException(username);
        }
        if (UserRoleEnum.ADMIN.equals(userRole) && !ReqInfoContext.getContext().isAdmin()) {
            ResultCode.FORBIDDEN.throwException("创建管理员账号需要管理员权限");
        }

        doCreateUser(normalizedEmail, username, password, userRole, false, null, null, null);
        log.info("管理员创建用户成功 email={} username={}", normalizedEmail, username);
    }

    @Override
    public void sendResetPasswordCode(String email) {
        String normalizedEmail = normalizeEmail(email);
        guardMailIp();
        guardMailSend(PURPOSE_RESET, normalizedEmail);

        if (userDAO.getByEmail(normalizedEmail) == null) {
            return;
        }

        String code = issueCode(RedisKeyConstants.getResetCodeKey(normalizedEmail));
        mailService.send(normalizedEmail, "重置密码验证码", codeHtml("重置密码", code));
    }

    @Override
    public void resetPassword(String email, String code, String newPassword) {
        String normalizedEmail = normalizeEmail(email);
        verifyCode(RedisKeyConstants.getResetCodeKey(normalizedEmail),
            RedisKeyConstants.getResetCodeAttemptsKey(normalizedEmail), code);

        UserDO user = userDAO.getByEmail(normalizedEmail);
        if (user == null) {
            ResultCode.USER_NOT_EXISTS.throwException();
        }
        userDAO.updatePassword(user.getId(), passwordEncoder.encode(newPassword));
        log.info("密码重置成功 userId={}", user.getId());
    }

    @Transactional(rollbackFor = Exception.class)
    @Override
    public UserCreatedVO adminCreateUser(AdminCreateUserParam req) {
        if (userDAO.existsUser(req.getUsername())) {
            ResultCode.USER_ALREADY_EXISTS.throwException(req.getUsername());
        }

        String normalizedEmail = normalizeEmail(req.getEmail());
        if (normalizedEmail != null && userDAO.existsEmail(normalizedEmail)) {
            ResultCode.USER_ALREADY_EXISTS.throwException(normalizedEmail);
        }

        String password = PasswordUtil.generateRandomPassword();
        doCreateUser(normalizedEmail, req.getUsername(), password, req.getUserRole(), false, req.getPosition(),
            req.getCompany(), req.getProfile());
        log.info("管理员创建用户成功 username={}", req.getUsername());
        return new UserCreatedVO().setUsername(req.getUsername()).setInitialPassword(password);
    }

    private Long doCreateUser(String email, String username, String rawPassword, UserRoleEnum role,
        boolean emailVerified, String position, String company, String profile) {
        UserDO newUser = new UserDO().setUsername(username).setEmail(email).setEmailVerified(emailVerified)
            .setPassword(passwordEncoder.encode(rawPassword)).setThirdAccountId("")
            .setLoginType(LoginTypeEnum.EMAIL_PASSWORD);
        userDAO.save(newUser);

        UserInfoDO userInfo = new UserInfoDO().setUserId(newUser.getId()).setNickname(username).setAvatar("")
            .setUserRole(role).setPosition(position).setCompany(company).setProfile(profile);
        userInfoDAO.save(userInfo);
        return newUser.getId();
    }

    private String normalizeEmail(String email) {
        return StringUtils.hasText(email) ? email.trim().toLowerCase(Locale.ROOT) : null;
    }

    /** 与邮箱无关，任何请求都先过，防止被当作邮件轰炸跳板，也限制枚举探测速率 */
    private void guardMailIp() {
        String clientIp = ReqInfoContext.getContext().getClientIp();
        if (clientIp != null) {
            guard(RedisKeyConstants.getMailIpKey(clientIp), MAIL_IP_MAX, MAIL_IP_WINDOW);
        }
    }

    /** 邮箱维度配额，只在确定要发信时才消耗 */
    private void guardMailSend(String purpose, String email) {
        guard(RedisKeyConstants.getMailCooldownKey(purpose, email), 1, MAIL_COOLDOWN);
        guard(RedisKeyConstants.getMailDailyKey(purpose, email), MAIL_DAILY_MAX, MAIL_DAILY_WINDOW);
    }

    private void guard(String key, int limit, Duration window) {
        RateLimiter.Result result = rateLimiter.tryAcquire(key, limit, window);
        if (!result.allowed()) {
            throw new BusinessException(ResultCode.LOGIN_TOO_MANY_ATTEMPTS.getCode(),
                "操作过于频繁，请 " + result.retryAfterSeconds() + " 秒后再试");
        }
    }

    private String issueCode(String key) {
        String code = String.valueOf(SECURE_RANDOM.nextInt(CODE_ORIGIN, CODE_BOUND));
        redisTemplate.opsForValue().set(key, code, CODE_TTL.toSeconds(), TimeUnit.SECONDS);
        return code;
    }

    /**
     * 校验验证码。6 位数字熵仅 10^6，哈希无意义，靠失败次数上限防爆破：累计错 {@value #CODE_MAX_ATTEMPTS} 次即作废该验证码。
     * 「未申请验证码」与「验证码填错」返回同一文案，否则可用于探测某邮箱是否正在注册或找回密码。
     */
    private void verifyCode(String codeKey, String attemptsKey, String input) {
        String stored = redisTemplate.opsForValue().get(codeKey);

        if (!rateLimiter.tryAcquire(attemptsKey, CODE_MAX_ATTEMPTS, CODE_TTL).allowed()) {
            redisTemplate.delete(List.of(codeKey, attemptsKey));
            ResultCode.INVALID_PARAMETER.throwException("验证码错误次数过多，请重新获取");
        }
        if (stored == null || !stored.equals(input)) {
            ResultCode.INVALID_PARAMETER.throwException("验证码无效或已过期");
        }

        redisTemplate.delete(List.of(codeKey, attemptsKey));
    }

    private String codeHtml(String purpose, String code) {
        return """
            <p>你的验证码是 <strong>%s</strong>，5 分钟内有效。</p>
            <p>请在页面中输入该验证码以%s。</p>
            <p>如果不是你本人操作，忽略这封邮件即可。</p>""".formatted(code, purpose);
    }


    @Override
    public LoginResult login(String email, String password) {
        String normalizedEmail = normalizeEmail(email);
        String clientIp = ReqInfoContext.getContext().getClientIp();
        loginRateLimiter.check(clientIp, normalizedEmail);

        UserDO user = userDAO.getByEmail(normalizedEmail);
        if (user == null || !passwordEncoder.matches(password, user.getPassword())) {
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
