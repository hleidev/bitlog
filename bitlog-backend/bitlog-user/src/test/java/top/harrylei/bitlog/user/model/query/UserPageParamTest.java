package top.harrylei.bitlog.user.model.query;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.EnumSource;
import top.harrylei.bitlog.user.model.enums.UserSortEnum;
import top.harrylei.bitlog.user.model.enums.UserStateEnum;
import top.harrylei.bitlog.user.model.enums.UserStatusEnum;
import top.harrylei.bitlog.common.enums.DeleteStatusEnum;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * 用户列表查询参数测试，覆盖账号状态维度到存储两列的翻译
 *
 * @author Harry
 * @since 2026-08-07
 */
class UserPageParamTest {

    /**
     * 注销是终态且只体现在 deleted 列上，默认列表若不排除就是墓碑账号泄漏
     */
    @Test
    @DisplayName("不传 state 时默认排除已注销账号")
    void getDeletedFilter_stateNotProvided_excludesDeactivated() {
        UserPageParam param = new UserPageParam();

        assertThat(param.getDeletedFilter()).isEqualTo(DeleteStatusEnum.NOT_DELETED);
        assertThat(param.getStatusFilter()).isNull();
    }

    @ParameterizedTest
    @EnumSource(value = UserStateEnum.class, names = {"ENABLED", "DISABLED"})
    @DisplayName("启用与禁用都只看未注销账号")
    void getDeletedFilter_activeStates_excludeDeactivated(UserStateEnum state) {
        UserPageParam param = new UserPageParam().setState(state);

        assertThat(param.getDeletedFilter()).isEqualTo(DeleteStatusEnum.NOT_DELETED);
    }

    @Test
    @DisplayName("已注销只按 deleted 过滤，不限制 status")
    void getStatusFilter_stateDeactivated_doesNotConstrainStatus() {
        UserPageParam param = new UserPageParam().setState(UserStateEnum.DEACTIVATED);

        assertThat(param.getDeletedFilter()).isEqualTo(DeleteStatusEnum.DELETED);
        assertThat(param.getStatusFilter()).as("注销不改 status，按 status 过滤会漏掉禁用后再注销的账号").isNull();
    }

    @Test
    @DisplayName("启用映射到 status=启用")
    void getStatusFilter_stateEnabled_returnsEnabled() {
        assertThat(new UserPageParam().setState(UserStateEnum.ENABLED).getStatusFilter())
            .isEqualTo(UserStatusEnum.ENABLED);
    }

    @Test
    @DisplayName("禁用映射到 status=禁用")
    void getStatusFilter_stateDisabled_returnsDisabled() {
        assertThat(new UserPageParam().setState(UserStateEnum.DISABLED).getStatusFilter())
            .isEqualTo(UserStatusEnum.DISABLED);
    }

    @ParameterizedTest
    @EnumSource(UserStateEnum.class)
    @DisplayName("每个状态取值都有确定的翻译结果，新增枚举值必须同步翻译逻辑")
    void translation_everyState_isExhaustivelyMapped(UserStateEnum state) {
        UserPageParam param = new UserPageParam().setState(state);

        assertThat(param.getDeletedFilter()).as("%s 未映射 deleted 条件", state).isNotNull();
    }

    @Test
    @DisplayName("列表按注册时间倒序，join 查询的次序键带表别名")
    void sortContract_userList_isAliasQualified() {
        UserPageParam param = new UserPageParam();

        assertThat(param.toPage().orders()).extracting(com.baomidou.mybatisplus.core.metadata.OrderItem::getColumn)
            .containsExactly(UserSortEnum.CREATE_TIME.getColumn(), "a.id");
    }
}
