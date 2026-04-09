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
import top.harrylei.bitlog.common.constans.RedisKeyConstants;
import top.harrylei.bitlog.common.context.ReqInfoContext;
import top.harrylei.bitlog.common.enums.ResultCode;
import top.harrylei.bitlog.user.config.JwtProperties;
import top.harrylei.bitlog.user.repository.dao.UserDAO;
import top.harrylei.bitlog.user.repository.dao.UserInfoDAO;
import top.harrylei.bitlog.user.repository.entity.UserDO;
import top.harrylei.bitlog.user.repository.entity.UserInfoDO;
import top.harrylei.bitlog.user.service.AuthService;
import top.harrylei.bitlog.user.service.LoginResult;
import top.harrylei.bitlog.user.util.JwtUtil;

import java.util.UUID;
import java.util.concurrent.TimeUnit;

/**
 * 认证服务实现
 *
 * @author harry
 * @since 0.0.1
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

    @Transactional(rollbackFor = Exception.class)
    @Override
    public void register(String username, String password, UserRoleEnum userRole) {
        if (userDAO.existsUser(username)) {
            ResultCode.USER_ALREADY_EXISTS.throwException(username);
        }

        if (UserRoleEnum.ADMIN.equals(userRole) && !ReqInfoContext.getContext().isAdmin()) {
            ResultCode.FORBIDDEN.throwException("创建管理员账号需要管理员权限");
        }

        UserDO newUser = new UserDO()
                .setUserName(username)
                .setPassword(passwordEncoder.encode(password))
                .setThirdAccountId("")
                .setLoginType(LoginTypeEnum.USERNAME_PASSWORD);
        userDAO.save(newUser);

        UserInfoDO userInfo = new UserInfoDO()
                .setUserId(newUser.getId())
                .setUserName(username)
                .setAvatar("")
                .setUserRole(userRole);
        userInfoDAO.save(userInfo);

        log.info("用户注册成功 userId={}", newUser.getId());
    }

    @Override
    public LoginResult login(String username, String password) {
        UserDO user = userDAO.getByUsername(username);
        if (user == null || !passwordEncoder.matches(password, user.getPassword())) {
            ResultCode.USERNAME_OR_PASSWORD_ERROR.throwException();
        }

        if (!UserStatusEnum.ENABLED.equals(user.getStatus())) {
            ResultCode.USER_DISABLED.throwException(username);
        }

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

    /**
     * 颁发双 Token：生成 Access Token + Refresh Token 并将 Refresh Token 存入 Redis
     */
    private LoginResult issueTokenPair(Long userId, UserRoleEnum role) {
        String accessToken = jwtUtil.generateToken(userId, role);
        String refreshToken = UUID.randomUUID().toString();
        String redisValue = userId + ":" + role.name();

        redisTemplate.opsForValue().set(
                RedisKeyConstants.getUserRefreshTokenKey(refreshToken),
                redisValue,
                jwtProperties.getRefreshTokenExpire().getSeconds(),
                TimeUnit.SECONDS
        );

        log.info("颁发 Token 对 userId={}", userId);
        return new LoginResult(accessToken, refreshToken);
    }
}
