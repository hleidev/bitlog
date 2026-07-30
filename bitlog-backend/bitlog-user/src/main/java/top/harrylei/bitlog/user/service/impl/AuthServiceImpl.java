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

    private final UserDAO userDAO;
    private final UserInfoDAO userInfoDAO;
    private final JwtUtil jwtUtil;
    private final JwtProperties jwtProperties;
    private final PasswordEncoder passwordEncoder;
    private final StringRedisTemplate redisTemplate;
    private final LoginRateLimiter loginRateLimiter;

    @Transactional(rollbackFor = Exception.class)
    @Override
    public void register(String email, String username, String password, UserRoleEnum userRole) {
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

        doCreateUser(normalizedEmail, username, password, userRole, null, null, null);
        log.info("用户注册成功 email={} username={}", normalizedEmail, username);
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
        doCreateUser(normalizedEmail, req.getUsername(), password, req.getUserRole(), req.getPosition(),
            req.getCompany(), req.getProfile());
        log.info("管理员创建用户成功 username={}", req.getUsername());
        return new UserCreatedVO().setUsername(req.getUsername()).setInitialPassword(password);
    }

    private void doCreateUser(String email, String username, String rawPassword, UserRoleEnum role, String position,
        String company, String profile) {
        UserDO newUser = new UserDO().setUsername(username).setEmail(email).setEmailVerified(false)
            .setPassword(passwordEncoder.encode(rawPassword)).setThirdAccountId("")
            .setLoginType(LoginTypeEnum.EMAIL_PASSWORD);
        userDAO.save(newUser);

        UserInfoDO userInfo = new UserInfoDO().setUserId(newUser.getId()).setNickname(username).setAvatar("")
            .setUserRole(role).setPosition(position).setCompany(company).setProfile(profile);
        userInfoDAO.save(userInfo);
    }

    private String normalizeEmail(String email) {
        return StringUtils.hasText(email) ? email.trim().toLowerCase(Locale.ROOT) : null;
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
