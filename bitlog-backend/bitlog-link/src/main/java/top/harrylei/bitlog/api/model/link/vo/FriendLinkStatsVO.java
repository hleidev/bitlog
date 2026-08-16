package top.harrylei.bitlog.api.model.link.vo;

import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Data;
import lombok.experimental.Accessors;

/**
 * 友链数量统计展示对象
 *
 * @author Harry
 * @since 2026-08-16
 */
@Data
@Accessors(chain = true)
@Schema(description = "友链数量统计展示对象")
public class FriendLinkStatsVO {

    @Schema(description = "友链总数（不含已删除）")
    private long total;

    @Schema(description = "待审核数")
    private long pending;

    @Schema(description = "展示中的数量")
    private long approved;

    @Schema(description = "未通过的数量")
    private long rejected;
}
