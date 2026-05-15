package top.harrylei.bitlog.user.service;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.data.redis.core.ValueOperations;
import org.springframework.security.crypto.password.PasswordEncoder;
import top.harrylei.bitlog.api.enums.user.UserRoleEnum;
import top.harrylei.bitlog.api.enums.user.UserStatusEnum;
import top.harrylei.bitlog.common.context.ReqInfoContext;
import top.harrylei.bitlog.common.exception.BusinessException;
import top.harrylei.bitlog.user.config.JwtProperties;
import top.harrylei.bitlog.user.repository.dao.UserDAO;
import top.harrylei.bitlog.user.repository.dao.UserInfoDAO;
import top.harrylei.bitlog.user.repository.entity.UserDO;
import top.harrylei.bitlog.user.repository.entity.UserInfoDO;
import top.harrylei.bitlog.user.service.impl.AuthServiceImpl;
import top.harrylei.bitlog.user.util.JwtUtil;

import java.time.Duration;
import java.util.List;
import java.util.concurrent.TimeUnit;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
@DisplayName("AuthServiceImpl 认证服务测试")
class AuthServiceImplTest {

    @Mock private UserDAO userDAO;
    @Mock private UserInfoDAO userInfoDAO;
    @Mock private JwtUtil jwtUtil;
    @Mock private JwtProperties jwtProperties;
    @Mock private PasswordEncoder passwordEncoder;
    @Mock private StringRedisTemplate redisTemplate;
    @Mock private ValueOperations<String, String> valueOperations;

    @InjectMocks
    private AuthServiceImpl authService;

    @BeforeEach
    void setUp() {
        ReqInfoContext.clear();
    }

    // ===== register =====

    @Test
    @DisplayName("register_用户名已存在_抛出BusinessException")
    void register_whenUsernameExists_throwsBusinessException() {
        when(userDAO.existsUser("existingUser")).thenReturn(true);

        assertThatThrownBy(() -> authService.register("existingUser", "password", UserRoleEnum.NORMAL))
                .isInstanceOf(BusinessException.class)
                .hasMessageContaining("用户已存在");

        verify(userInfoDAO, never()).save(any());
    }

    @Test
    @DisplayName("register_正常注册普通用户_保存用户账号和信息")
    void register_withValidInput_savesUserAndUserInfo() {
        String username = "newUser";
        String password = "rawPassword";
        String encodedPassword = "encodedPassword";

        when(userDAO.existsUser(username)).thenReturn(false);
        when(passwordEncoder.encode(password)).thenReturn(encodedPassword);
        when(userDAO.save(any(UserDO.class))).thenReturn(true);
        when(userInfoDAO.save(any(UserInfoDO.class))).thenReturn(true);

        authService.register(username, password, UserRoleEnum.NORMAL);

        verify(userDAO).save(argThat(user ->
                user.getUsername().equals(username) &&
                user.getPassword().equals(encodedPassword)
        ));
        verify(userInfoDAO).save(argThat(info ->
                info.getNickname().equals(username) &&
                info.getUserRole().equals(UserRoleEnum.NORMAL)
        ));
    }

    @Test
    @DisplayName("register_非管理员创建管理员账号_抛出BusinessException")
    void register_normalUserCreatesAdmin_throwsForbiddenException() {
        when(userDAO.existsUser("adminUser")).thenReturn(false);

        ReqInfoContext.ReqInfo reqInfo = new ReqInfoContext.ReqInfo();
        reqInfo.setUserId(10L);
        ReqInfoContext.setContext(reqInfo);

        assertThatThrownBy(() -> authService.register("adminUser", "password", UserRoleEnum.ADMIN))
                .isInstanceOf(BusinessException.class)
                .hasMessageContaining("权限不足");

        verify(userDAO, never()).save(any());
    }

    @Test
    @DisplayName("register_管理员创建管理员账号_注册成功")
    void register_adminUserCreatesAdmin_succeeds() {
        String username = "newAdmin";

        ReqInfoContext.ReqInfo reqInfo = new ReqInfoContext.ReqInfo();
        reqInfo.setUserId(1L);
        reqInfo.setAuthorities(List.of(ReqInfoContext.ROLE_ADMIN));
        ReqInfoContext.setContext(reqInfo);

        when(userDAO.existsUser(username)).thenReturn(false);
        when(passwordEncoder.encode(anyString())).thenReturn("encoded");
        when(userDAO.save(any(UserDO.class))).thenReturn(true);
        when(userInfoDAO.save(any(UserInfoDO.class))).thenReturn(true);

        authService.register(username, "password", UserRoleEnum.ADMIN);

        verify(userInfoDAO).save(argThat(info -> info.getUserRole().equals(UserRoleEnum.ADMIN)));
    }

    // ===== login =====

    @Test
    @DisplayName("login_用户不存在_抛出BusinessException")
    void login_whenUserNotFound_throwsBusinessException() {
        when(userDAO.getByUsername("unknown")).thenReturn(null);

        assertThatThrownBy(() -> authService.login("unknown", "pass"))
                .isInstanceOf(BusinessException.class)
                .hasMessageContaining("用户名或密码错误");
    }

    @Test
    @DisplayName("login_密码错误_抛出BusinessException")
    void login_withWrongPassword_throwsBusinessException() {
        String username = "harry";
        UserDO user = buildEnabledUser(username, "encodedPass");
        when(userDAO.getByUsername(username)).thenReturn(user);
        when(passwordEncoder.matches("wrongPass", "encodedPass")).thenReturn(false);

        assertThatThrownBy(() -> authService.login(username, "wrongPass"))
                .isInstanceOf(BusinessException.class)
                .hasMessageContaining("用户名或密码错误");
    }

    @Test
    @DisplayName("login_用户已禁用_抛出BusinessException")
    void login_withDisabledUser_throwsBusinessException() {
        String username = "disabledUser";
        UserDO user = new UserDO();
        user.setId(10L);
        user.setUsername(username);
        user.setPassword("encodedPass");
        user.setStatus(UserStatusEnum.DISABLED);
        when(userDAO.getByUsername(username)).thenReturn(user);
        when(passwordEncoder.matches("pass", "encodedPass")).thenReturn(true);

        assertThatThrownBy(() -> authService.login(username, "pass"))
                .isInstanceOf(BusinessException.class)
                .hasMessageContaining("用户已被禁用");
    }

    @Test
    @DisplayName("login_正常登录_返回AccessToken和RefreshToken并写入Redis")
    void login_withValidCredentials_returnsTokenPairAndCachesRefreshInRedis() {
        String username = "harry";
        Long userId = 100L;
        String expectedAccessToken = "jwt.access.token";

        UserDO user = buildEnabledUser(username, "encodedPass");
        user.setId(userId);

        UserInfoDO userInfo = new UserInfoDO();
        userInfo.setUserId(userId);
        userInfo.setUserRole(UserRoleEnum.NORMAL);

        when(userDAO.getByUsername(username)).thenReturn(user);
        when(passwordEncoder.matches("rawPass", "encodedPass")).thenReturn(true);
        when(userInfoDAO.getByUserId(userId)).thenReturn(userInfo);
        when(jwtUtil.generateToken(userId, UserRoleEnum.NORMAL)).thenReturn(expectedAccessToken);
        when(jwtProperties.getRefreshTokenExpire()).thenReturn(Duration.ofDays(30));
        when(redisTemplate.opsForValue()).thenReturn(valueOperations);

        LoginResult result = authService.login(username, "rawPass");

        assertThat(result.accessToken()).isEqualTo(expectedAccessToken);
        assertThat(result.refreshToken()).isNotBlank();
        verify(valueOperations).set(
                contains(result.refreshToken()),
                eq(userId + ":" + UserRoleEnum.NORMAL.name()),
                eq(Duration.ofDays(30).getSeconds()),
                eq(TimeUnit.SECONDS)
        );
    }

    @Test
    @DisplayName("login_userInfo为null时_默认角色为NORMAL")
    void login_whenUserInfoIsNull_defaultsToNormalRole() {
        String username = "harry";
        Long userId = 100L;

        UserDO user = buildEnabledUser(username, "encodedPass");
        user.setId(userId);

        when(userDAO.getByUsername(username)).thenReturn(user);
        when(passwordEncoder.matches("rawPass", "encodedPass")).thenReturn(true);
        when(userInfoDAO.getByUserId(userId)).thenReturn(null);
        when(jwtUtil.generateToken(userId, UserRoleEnum.NORMAL)).thenReturn("token");
        when(jwtProperties.getRefreshTokenExpire()).thenReturn(Duration.ofDays(30));
        when(redisTemplate.opsForValue()).thenReturn(valueOperations);

        authService.login(username, "rawPass");

        verify(jwtUtil).generateToken(userId, UserRoleEnum.NORMAL);
    }

    // ===== refresh =====

    @Test
    @DisplayName("refresh_RefreshToken无效_抛出BusinessException")
    void refresh_withInvalidToken_throwsBusinessException() {
        String token = "invalid-token";
        when(redisTemplate.opsForValue()).thenReturn(valueOperations);
        when(valueOperations.get(contains(token))).thenReturn(null);

        assertThatThrownBy(() -> authService.refresh(token))
                .isInstanceOf(BusinessException.class)
                .hasMessageContaining("Refresh Token 无效或已过期");
    }

    @Test
    @DisplayName("refresh_有效RefreshToken_轮换后返回新Token对")
    void refresh_withValidToken_rotatesAndReturnsNewTokenPair() {
        String oldToken = "old-refresh-token";
        String newAccessToken = "new.access.token";

        when(redisTemplate.opsForValue()).thenReturn(valueOperations);
        when(valueOperations.get(contains(oldToken))).thenReturn("100:NORMAL");
        when(redisTemplate.delete(contains(oldToken))).thenReturn(true);
        when(userDAO.getById(100L)).thenReturn(buildEnabledUser("testUser", "encodedPass"));
        when(jwtUtil.generateToken(100L, UserRoleEnum.NORMAL)).thenReturn(newAccessToken);
        when(jwtProperties.getRefreshTokenExpire()).thenReturn(Duration.ofDays(30));

        LoginResult result = authService.refresh(oldToken);

        assertThat(result.accessToken()).isEqualTo(newAccessToken);
        assertThat(result.refreshToken()).isNotBlank();
        assertThat(result.refreshToken()).isNotEqualTo(oldToken);
        verify(redisTemplate).delete(contains(oldToken));
    }

    // ===== logout =====

    @Test
    @DisplayName("logout_有效RefreshToken_从Redis中删除")
    void logout_withValidToken_deletesFromRedis() {
        String refreshToken = "valid-refresh-token";
        when(redisTemplate.delete(contains(refreshToken))).thenReturn(true);

        authService.logout(refreshToken);

        verify(redisTemplate).delete(contains(refreshToken));
    }

    @Test
    @DisplayName("logout_RefreshToken为null_不操作Redis")
    void logout_withNullToken_doesNothing() {
        authService.logout(null);

        verify(redisTemplate, never()).delete(anyString());
    }

    @Test
    @DisplayName("logout_Redis删除抛异常_不向上抛出")
    void logout_whenRedisThrowsException_doesNotPropagateException() {
        String refreshToken = "some-token";
        when(redisTemplate.delete(anyString())).thenThrow(new RuntimeException("Redis连接失败"));

        authService.logout(refreshToken);
    }

    // ===== 辅助方法 =====

    private UserDO buildEnabledUser(String username, String encodedPassword) {
        UserDO user = new UserDO();
        user.setUsername(username);
        user.setPassword(encodedPassword);
        user.setStatus(UserStatusEnum.ENABLED);
        return user;
    }
}
