package top.harrylei.bitlog.api.model.link.query;

import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.Size;
import lombok.Data;
import lombok.EqualsAndHashCode;
import lombok.experimental.Accessors;
import top.harrylei.bitlog.api.enums.link.FriendLinkSortEnum;
import top.harrylei.bitlog.api.enums.link.FriendLinkStatusEnum;
import top.harrylei.bitlog.common.model.BasePage;
import top.harrylei.bitlog.common.model.SortField;

/**
 * 友链管理列表查询参数
 *
 * @author Harry
 * @since 2026-08-15
 */
@Data
@EqualsAndHashCode(callSuper = true)
@Accessors(chain = true)
@Schema(description = "友链管理列表查询参数")
public class FriendLinkPageParam extends BasePage {

    @Schema(description = "状态：0-待审核，1-已通过，2-已拒绝")
    private FriendLinkStatusEnum status;

    @Size(max = 50, message = "关键词长度不能超过 50")
    @Schema(description = "关键词（搜索站点名称与地址）")
    private String keyword;

    @Override
    protected SortField resolveSortField() {
        return FriendLinkSortEnum.CREATE_TIME;
    }
}
