package top.harrylei.bitlog.user.model.vo;

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

    @Schema(description = "用户总数（不含已注销）")
    private long total;

    @Schema(description = "启用用户数（不含已注销）")
    private long enabled;

    @Schema(description = "禁用用户数（不含已注销）")
    private long disabled;

    @Schema(description = "已注销用户数")
    private long deactivated;
}
