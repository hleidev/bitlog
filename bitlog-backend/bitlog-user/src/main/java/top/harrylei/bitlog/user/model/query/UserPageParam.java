package top.harrylei.bitlog.user.model.query;

import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.Size;
import lombok.Data;
import lombok.EqualsAndHashCode;
import lombok.experimental.Accessors;
import top.harrylei.bitlog.user.model.enums.UserSortEnum;
import top.harrylei.bitlog.user.model.enums.UserStateEnum;
import top.harrylei.bitlog.user.model.enums.UserStatusEnum;
import top.harrylei.bitlog.common.enums.DeleteStatusEnum;
import top.harrylei.bitlog.common.model.BasePage;
import top.harrylei.bitlog.common.model.SortField;

/**
 * 用户列表查询参数
 *
 * @author Harry
 * @since 2026-03-17
 */
@Data
@EqualsAndHashCode(callSuper = true)
@Schema(description = "用户列表查询参数")
@Accessors(chain = true)
public class UserPageParam extends BasePage {

    @Size(max = 50, message = "关键词长度不能超过 50")
    @Schema(description = "关键词（搜索用户名）")
    private String keyword;

    @Schema(description = "账号状态：ENABLED-启用，DISABLED-禁用，DEACTIVATED-已注销；不传返回全部未注销账号")
    private UserStateEnum state;

    @Override
    protected SortField resolveSortField() {
        return UserSortEnum.CREATE_TIME;
    }

    @Override
    protected String tieBreaker() {
        return "a.id";
    }

    /** state 翻译：账号启停过滤值，null 表示不过滤 */
    @Schema(hidden = true)
    public UserStatusEnum getStatusFilter() {
        if (state == null) {
            return null;
        }
        return switch (state) {
            case ENABLED -> UserStatusEnum.ENABLED;
            case DISABLED -> UserStatusEnum.DISABLED;
            case DEACTIVATED -> null;
        };
    }

    /** state 翻译：注销标记过滤值，不传 state 时默认排除注销账号 */
    @Schema(hidden = true)
    public DeleteStatusEnum getDeletedFilter() {
        return state == UserStateEnum.DEACTIVATED ? DeleteStatusEnum.DELETED : DeleteStatusEnum.NOT_DELETED;
    }
}
