package top.harrylei.bitlog.api.model.user.vo;

import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Data;
import lombok.experimental.Accessors;

/**
 * 用户数量统计展示对象
 *
 * @author Harry
 * @since 2026-04-10
 */
@Data
@Accessors(chain = true)
@Schema(description = "用户数量统计展示对象")
public class UserStatsVO {

    @Schema(description = "未删除用户总数")
    private long total;

    @Schema(description = "启用用户数（未删除）")
    private long enabled;

    @Schema(description = "禁用用户数（未删除）")
    private long disabled;

    @Schema(description = "已删除用户数")
    private long deleted;
}
