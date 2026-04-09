package top.harrylei.bitlog.user.service;

import com.baomidou.mybatisplus.core.conditions.update.LambdaUpdateWrapper;
import com.baomidou.mybatisplus.extension.conditions.update.LambdaUpdateChainWrapper;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.security.crypto.password.PasswordEncoder;
import top.harrylei.bitlog.api.enums.user.UserRoleEnum;
import top.harrylei.bitlog.api.enums.user.UserStatusEnum;
import top.harrylei.bitlog.api.model.user.req.PasswordUpdateRequest;
import top.harrylei.bitlog.api.model.user.req.UserUpdateRequest;
import top.harrylei.bitlog.api.model.user.vo.UserDetailVO;
import top.harrylei.bitlog.api.model.user.vo.UserVO;
import top.harrylei.bitlog.common.exception.BusinessException;
import top.harrylei.bitlog.user.converter.UserConverter;
import top.harrylei.bitlog.user.converter.UserConverterImpl;
import top.harrylei.bitlog.user.repository.dao.UserDAO;
import top.harrylei.bitlog.user.repository.dao.UserInfoDAO;
import top.harrylei.bitlog.user.repository.entity.UserDO;
import top.harrylei.bitlog.user.repository.entity.UserInfoDO;
import top.harrylei.bitlog.user.service.impl.UserServiceImpl;

import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
@DisplayName("UserServiceImpl 用户服务测试")
class UserServiceImplTest {

    @Mock
    private UserDAO userDAO;

    @Mock
    private UserInfoDAO userInfoDAO;

    @Mock
    private PasswordEncoder passwordEncoder;

    private final UserConverter userConverter = new UserConverterImpl();

    @InjectMocks
    private UserServiceImpl userService;

    // 由于 @InjectMocks 注入的是 Mock，UserConverter 需要手动注入真实实现
    // 通过反射注入，或重构为 @BeforeEach
    @org.junit.jupiter.api.BeforeEach
    void injectConverter() throws Exception {
        var field = UserServiceImpl.class.getDeclaredField("userConverter");
        field.setAccessible(true);
        field.set(userService, userConverter);
    }

    // ===== getUserById =====

    @Test
    @DisplayName("getUserById_userId为null_返回null")
    void getUserById_withNullUserId_returnsNull() {
        UserVO result = userService.getUserById(null);

        assertThat(result).isNull();
        verifyNoInteractions(userInfoDAO);
    }

    @Test
    @DisplayName("getUserById_userInfo不存在_返回null")
    void getUserById_whenUserInfoNotFound_returnsNull() {
        when(userInfoDAO.getByUserId(999L)).thenReturn(null);

        UserVO result = userService.getUserById(999L);

        assertThat(result).isNull();
        verifyNoInteractions(userDAO);
    }

    @Test
    @DisplayName("getUserById_user账号数据缺失_降级返回仅info字段的VO")
    void getUserById_whenUserAccountMissing_returnsDegradedVO() {
        Long userId = 100L;
        UserInfoDO userInfo = buildUserInfoDO(userId, "harry", UserRoleEnum.NORMAL);
        userInfo.setId(100L);

        when(userInfoDAO.getByUserId(userId)).thenReturn(userInfo);
        when(userDAO.getById(userInfo.getId())).thenReturn(null);

        UserVO result = userService.getUserById(userId);

        assertThat(result).isNotNull();
        assertThat(result.getUserId()).isEqualTo(userId);
        assertThat(result.getUserName()).isEqualTo("harry");
        assertThat(result.getEmail()).isNull();
    }

    @Test
    @DisplayName("getUserById_正常返回_包含email字段")
    void getUserById_withBothUserInfoAndUser_returnsFullVO() {
        Long userId = 100L;
        UserInfoDO userInfo = buildUserInfoDO(userId, "harry", UserRoleEnum.NORMAL);
        userInfo.setId(100L);

        UserDO user = new UserDO();
        user.setEmail("harry@bitlog.top");
        user.setStatus(UserStatusEnum.ENABLED);

        when(userInfoDAO.getByUserId(userId)).thenReturn(userInfo);
        when(userDAO.getById(userInfo.getId())).thenReturn(user);

        UserVO result = userService.getUserById(userId);

        assertThat(result).isNotNull();
        assertThat(result.getUserId()).isEqualTo(userId);
        assertThat(result.getEmail()).isEqualTo("harry@bitlog.top");
    }

    // ===== getUserBatchByIds =====

    @Test
    @DisplayName("getUserBatchByIds_空列表_返回空列表")
    void getUserBatchByIds_withEmptyList_returnsEmptyList() {
        assertThat(userService.getUserBatchByIds(List.of())).isEmpty();
        assertThat(userService.getUserBatchByIds(null)).isEmpty();
        verifyNoInteractions(userInfoDAO);
    }

    @Test
    @DisplayName("getUserBatchByIds_userInfo列表为空_返回空列表")
    void getUserBatchByIds_whenUserInfoListEmpty_returnsEmptyList() {
        when(userInfoDAO.listByUserIds(anyList())).thenReturn(List.of());

        List<UserVO> result = userService.getUserBatchByIds(List.of(1L, 2L));

        assertThat(result).isEmpty();
    }

    // ===== getUserDetail =====

    @Test
    @DisplayName("getUserDetail_userInfo不存在_抛出BusinessException")
    void getUserDetail_whenUserInfoNotFound_throwsBusinessException() {
        when(userInfoDAO.getByUserId(999L)).thenReturn(null);

        assertThatThrownBy(() -> userService.getUserDetail(999L))
                .isInstanceOf(BusinessException.class)
                .hasMessageContaining("用户不存在");
    }

    @Test
    @DisplayName("getUserDetail_user账号不存在_抛出BusinessException")
    void getUserDetail_whenUserAccountNotFound_throwsBusinessException() {
        Long userId = 100L;
        UserInfoDO userInfo = buildUserInfoDO(userId, "harry", UserRoleEnum.NORMAL);
        userInfo.setId(100L);

        when(userInfoDAO.getByUserId(userId)).thenReturn(userInfo);
        when(userDAO.getById(userInfo.getId())).thenReturn(null);

        assertThatThrownBy(() -> userService.getUserDetail(userId))
                .isInstanceOf(BusinessException.class)
                .hasMessageContaining("用户不存在");
    }

    @Test
    @DisplayName("getUserDetail_正常返回_包含完整信息")
    void getUserDetail_withValidUser_returnsDetailVO() {
        Long userId = 100L;
        UserInfoDO userInfo = buildUserInfoDO(userId, "harry", UserRoleEnum.NORMAL);
        userInfo.setId(100L);
        userInfo.setProfile("I love coding");
        userInfo.setPosition("Backend Engineer");
        userInfo.setCompany("Bytelogs");

        UserDO user = new UserDO();
        user.setEmail("harry@bitlog.top");
        user.setStatus(UserStatusEnum.ENABLED);

        when(userInfoDAO.getByUserId(userId)).thenReturn(userInfo);
        when(userDAO.getById(userInfo.getId())).thenReturn(user);

        UserDetailVO result = userService.getUserDetail(userId);

        assertThat(result).isNotNull();
        assertThat(result.getUserId()).isEqualTo(userId);
        assertThat(result.getEmail()).isEqualTo("harry@bitlog.top");
        assertThat(result.getProfile()).isEqualTo("I love coding");
        assertThat(result.getPosition()).isEqualTo("Backend Engineer");
    }

    // ===== updateUserInfo =====

    @Test
    @DisplayName("updateUserInfo_userInfo不存在_抛出BusinessException")
    void updateUserInfo_whenUserInfoNotFound_throwsBusinessException() {
        Long userId = 999L;
        when(userInfoDAO.getByUserId(userId)).thenReturn(null);

        UserUpdateRequest req = new UserUpdateRequest();
        req.setUserName("newName");

        assertThatThrownBy(() -> userService.updateUserInfo(userId, req))
                .isInstanceOf(BusinessException.class)
                .hasMessageContaining("用户不存在");
    }

    // ===== updatePassword =====

    @Test
    @DisplayName("updatePassword_userInfo不存在_抛出BusinessException")
    void updatePassword_whenUserInfoNotFound_throwsBusinessException() {
        Long userId = 999L;
        when(userInfoDAO.getByUserId(userId)).thenReturn(null);

        PasswordUpdateRequest req = new PasswordUpdateRequest();
        req.setOldPassword("old");
        req.setNewPassword("new");

        assertThatThrownBy(() -> userService.updatePassword(userId, req))
                .isInstanceOf(BusinessException.class)
                .hasMessageContaining("用户不存在");
    }

    @Test
    @DisplayName("updatePassword_user账号不存在_抛出BusinessException")
    void updatePassword_whenUserAccountNotFound_throwsBusinessException() {
        Long userId = 100L;
        UserInfoDO userInfo = buildUserInfoDO(userId, "harry", UserRoleEnum.NORMAL);
        userInfo.setId(100L);

        when(userInfoDAO.getByUserId(userId)).thenReturn(userInfo);
        when(userDAO.getById(userInfo.getId())).thenReturn(null);

        PasswordUpdateRequest req = new PasswordUpdateRequest();
        req.setOldPassword("old");
        req.setNewPassword("new");

        assertThatThrownBy(() -> userService.updatePassword(userId, req))
                .isInstanceOf(BusinessException.class)
                .hasMessageContaining("用户不存在");
    }

    @Test
    @DisplayName("updatePassword_旧密码错误_抛出BusinessException")
    void updatePassword_withWrongOldPassword_throwsBusinessException() {
        Long userId = 100L;
        UserInfoDO userInfo = buildUserInfoDO(userId, "harry", UserRoleEnum.NORMAL);
        userInfo.setId(100L);

        UserDO user = new UserDO();
        user.setId(100L);
        user.setPassword("encodedOldPass");

        when(userInfoDAO.getByUserId(userId)).thenReturn(userInfo);
        when(userDAO.getById(userInfo.getId())).thenReturn(user);
        when(passwordEncoder.matches("wrongOldPass", "encodedOldPass")).thenReturn(false);

        PasswordUpdateRequest req = new PasswordUpdateRequest();
        req.setOldPassword("wrongOldPass");
        req.setNewPassword("newPass");

        assertThatThrownBy(() -> userService.updatePassword(userId, req))
                .isInstanceOf(BusinessException.class)
                .hasMessageContaining("用户名或密码错误");
    }

    // ===== 辅助方法 =====

    private UserInfoDO buildUserInfoDO(Long userId, String userName, UserRoleEnum role) {
        UserInfoDO userInfo = new UserInfoDO();
        userInfo.setUserId(userId);
        userInfo.setUserName(userName);
        userInfo.setUserRole(role);
        return userInfo;
    }
}
