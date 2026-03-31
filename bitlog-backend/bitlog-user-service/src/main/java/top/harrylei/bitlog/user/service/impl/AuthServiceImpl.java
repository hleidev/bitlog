package top.harrylei.bitlog.user.service.impl;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import top.harrylei.bitlog.common.enums.ResultCode;
import top.harrylei.bitlog.api.enums.user.LoginTypeEnum;
import top.harrylei.bitlog.api.enums.user.UserRoleEnum;
import top.harrylei.bitlog.api.enums.user.UserStatusEnum;
import top.harrylei.bitlog.common.constans.RedisKeyConstants;
import top.harrylei.bitlog.common.context.ReqInfoContext;
import top.harrylei.bitlog.user.config.JwtProperties;
import top.harrylei.bitlog.user.repository.dao.UserDAO;
import top.harrylei.bitlog.user.repository.dao.UserInfoDAO;
import top.harrylei.bitlog.user.repository.entity.UserDO;
import top.harrylei.bitlog.user.repository.entity.UserInfoDO;
import top.harrylei.bitlog.user.service.AuthService;
import top.harrylei.bitlog.user.util.JwtUtil;

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
        if (userDAO.getByUsername(username) != null) {
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
    public String login(String username, String password, boolean keepLogin) {
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

        String token = jwtUtil.generateToken(userId, role, keepLogin);

        long expireSeconds = keepLogin
                ? jwtProperties.getKeepLoginExpire().getSeconds()
                : jwtProperties.getDefaultExpire().getSeconds();
        redisTemplate.opsForValue().set(
                RedisKeyConstants.getUserTokenKey(userId), token, expireSeconds, TimeUnit.SECONDS);

        log.info("用户登录成功 userId={}", userId);
        return token;
    }

    @Override
    public void logout(Long userId) {
        if (userId == null) {
            ResultCode.INVALID_PARAMETER.throwException("用户 ID 不能为空");
        }
        try {
            redisTemplate.delete(RedisKeyConstants.getUserTokenKey(userId));
            log.info("用户退出登录 userId={}", userId);
        } catch (Exception e) {
            log.error("退出登录异常 userId={}", userId, e);
        }
    }
}
