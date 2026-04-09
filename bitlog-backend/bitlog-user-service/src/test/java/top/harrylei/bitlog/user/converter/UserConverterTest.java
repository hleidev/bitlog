package top.harrylei.bitlog.user.converter;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import top.harrylei.bitlog.common.enums.DeleteStatusEnum;
import top.harrylei.bitlog.api.enums.user.UserRoleEnum;
import top.harrylei.bitlog.api.enums.user.UserStatusEnum;
import top.harrylei.bitlog.api.model.user.dto.UserDetailDTO;
import top.harrylei.bitlog.api.model.user.vo.UserDetailVO;
import top.harrylei.bitlog.api.model.user.vo.UserListVO;
import top.harrylei.bitlog.api.model.user.vo.UserVO;
import top.harrylei.bitlog.user.repository.entity.UserDO;
import top.harrylei.bitlog.user.repository.entity.UserInfoDO;

import java.time.LocalDateTime;

import static org.assertj.core.api.Assertions.assertThat;

@DisplayName("UserConverter 用户对象转换器测试")
class UserConverterTest {

    private UserConverter userConverter;

    @BeforeEach
    void setUp() {
        userConverter = new UserConverterImpl();
    }

    // ===== toVO(UserInfoDO, UserDO) =====

    @Test
    @DisplayName("toVO_userInfo和user均有值_所有字段正确映射")
    void toVO_withBothUserInfoAndUser_mapsAllFields() {
        UserInfoDO userInfo = buildUserInfoDO(100L, "harry", "https://cdn/avatar.jpg",
                "backend engineer", "Bytelogs Inc", "I love coding", UserRoleEnum.NORMAL);
        UserDO user = buildUserDO("harry@bitlog.top", UserStatusEnum.ENABLED);

        UserVO vo = userConverter.toVO(userInfo, user);

        assertThat(vo).isNotNull();
        assertThat(vo.getUserId()).isEqualTo(100L);
        assertThat(vo.getUserName()).isEqualTo("harry");
        assertThat(vo.getAvatar()).isEqualTo("https://cdn/avatar.jpg");
        assertThat(vo.getProfile()).isEqualTo("I love coding");
        assertThat(vo.getPosition()).isEqualTo("backend engineer");
        assertThat(vo.getCompany()).isEqualTo("Bytelogs Inc");
        assertThat(vo.getUserRole()).isEqualTo(UserRoleEnum.NORMAL);
        assertThat(vo.getEmail()).isEqualTo("harry@bitlog.top");
    }

    @Test
    @DisplayName("toVO_仅userInfo_email字段为null")
    void toVO_withOnlyUserInfo_emailIsNull() {
        UserInfoDO userInfo = buildUserInfoDO(200L, "alice", "", "", "", "", UserRoleEnum.ADMIN);

        UserVO vo = userConverter.toVO(userInfo);

        assertThat(vo).isNotNull();
        assertThat(vo.getUserId()).isEqualTo(200L);
        assertThat(vo.getUserName()).isEqualTo("alice");
        assertThat(vo.getUserRole()).isEqualTo(UserRoleEnum.ADMIN);
        assertThat(vo.getEmail()).isNull();
    }

    @Test
    @DisplayName("toVO_userInfo为null_user为null_返回null")
    void toVO_withBothNull_returnsNull() {
        assertThat(userConverter.toVO(null, null)).isNull();
    }

    @Test
    @DisplayName("toVO_单参数_null输入_返回null")
    void toVO_withNullUserInfo_returnsNull() {
        assertThat(userConverter.toVO((UserInfoDO) null)).isNull();
    }

    // ===== toDetailVO =====

    @Test
    @DisplayName("toDetailVO_所有字段_正确映射含状态和时间")
    void toDetailVO_withAllFields_mapsStatusAndTime() {
        LocalDateTime now = LocalDateTime.now();
        UserInfoDO userInfo = buildUserInfoDO(300L, "bob", "avatar", "dev", "company", "profile", UserRoleEnum.NORMAL);
        UserDO user = buildUserDO("bob@bitlog.top", UserStatusEnum.ENABLED);
        user.setDeleted(DeleteStatusEnum.NOT_DELETED);
        user.setCreateTime(now);
        user.setUpdateTime(now);

        UserDetailVO vo = userConverter.toDetailVO(userInfo, user);

        assertThat(vo).isNotNull();
        assertThat(vo.getUserId()).isEqualTo(300L);
        assertThat(vo.getUserName()).isEqualTo("bob");
        assertThat(vo.getEmail()).isEqualTo("bob@bitlog.top");
        assertThat(vo.getStatus()).isEqualTo(UserStatusEnum.ENABLED.getCode());
        assertThat(vo.getDeleted()).isEqualTo(DeleteStatusEnum.NOT_DELETED);
        assertThat(vo.getCreateTime()).isEqualTo(now);
        assertThat(vo.getUpdateTime()).isEqualTo(now);
        assertThat(vo.getPosition()).isEqualTo("dev");
        assertThat(vo.getCompany()).isEqualTo("company");
        assertThat(vo.getProfile()).isEqualTo("profile");
    }

    @Test
    @DisplayName("toDetailVO_禁用用户_status映射为0")
    void toDetailVO_withDisabledUser_statusIsZero() {
        UserInfoDO userInfo = buildUserInfoDO(400L, "carol", "", "", "", "", UserRoleEnum.NORMAL);
        UserDO user = buildUserDO("carol@bitlog.top", UserStatusEnum.DISABLED);

        UserDetailVO vo = userConverter.toDetailVO(userInfo, user);

        assertThat(vo).isNotNull();
        assertThat(vo.getStatus()).isEqualTo(UserStatusEnum.DISABLED.getCode());
    }

    @Test
    @DisplayName("toDetailVO_两参数均null_返回null")
    void toDetailVO_withBothNull_returnsNull() {
        assertThat(userConverter.toDetailVO(null, null)).isNull();
    }

    // ===== toListVO(UserDetailDTO) =====

    @Test
    @DisplayName("toListVO_DTO_所有字段正确映射")
    void toListVO_withUserDetailDTO_mapsAllFields() {
        LocalDateTime now = LocalDateTime.now();
        UserDetailDTO dto = new UserDetailDTO();
        dto.setUserId(500L);
        dto.setUserName("dave");
        dto.setEmail("dave@bitlog.top");
        dto.setStatus(1);
        dto.setDeleted(DeleteStatusEnum.NOT_DELETED);
        dto.setUserRole(UserRoleEnum.NORMAL);
        dto.setAvatar("avatar_url");
        dto.setCreateTime(now);
        dto.setUpdateTime(now);

        UserListVO vo = userConverter.toListVO(dto);

        assertThat(vo).isNotNull();
        assertThat(vo.getUserId()).isEqualTo(500L);
        assertThat(vo.getUserName()).isEqualTo("dave");
        assertThat(vo.getEmail()).isEqualTo("dave@bitlog.top");
        assertThat(vo.getStatus()).isEqualTo(1);
        assertThat(vo.getDeleted()).isEqualTo(DeleteStatusEnum.NOT_DELETED);
        assertThat(vo.getUserRole()).isEqualTo(UserRoleEnum.NORMAL);
        assertThat(vo.getAvatar()).isEqualTo("avatar_url");
        assertThat(vo.getCreateTime()).isEqualTo(now);
        assertThat(vo.getUpdateTime()).isEqualTo(now);
    }

    @Test
    @DisplayName("toListVO_null DTO_返回null")
    void toListVO_withNullDto_returnsNull() {
        assertThat(userConverter.toListVO((UserDetailDTO) null)).isNull();
    }

    // ===== map(UserStatusEnum) =====

    @Test
    @DisplayName("map_ENABLED状态_返回1")
    void map_withEnabledStatus_returnsOne() {
        assertThat(userConverter.map(UserStatusEnum.ENABLED)).isEqualTo(1);
    }

    @Test
    @DisplayName("map_DISABLED状态_返回0")
    void map_withDisabledStatus_returnsZero() {
        assertThat(userConverter.map(UserStatusEnum.DISABLED)).isEqualTo(0);
    }

    @Test
    @DisplayName("map_null状态_返回null")
    void map_withNullStatus_returnsNull() {
        assertThat(userConverter.map(null)).isNull();
    }

    // ===== 辅助方法 =====

    private UserInfoDO buildUserInfoDO(Long userId, String userName, String avatar,
                                       String position, String company, String profile,
                                       UserRoleEnum role) {
        UserInfoDO userInfo = new UserInfoDO();
        userInfo.setUserId(userId);
        userInfo.setUserName(userName);
        userInfo.setAvatar(avatar);
        userInfo.setPosition(position);
        userInfo.setCompany(company);
        userInfo.setProfile(profile);
        userInfo.setUserRole(role);
        return userInfo;
    }

    private UserDO buildUserDO(String email, UserStatusEnum status) {
        UserDO user = new UserDO();
        user.setEmail(email);
        user.setStatus(status);
        return user;
    }
}
