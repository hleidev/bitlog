package top.harrylei.bitlog.notification.model.vo;

import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Data;
import lombok.experimental.Accessors;

/**
 * 通知触发者视图对象
 *
 * @author Harry
 * @since 2026-09-07
 */
@Data
@Accessors(chain = true)
@Schema(description = "通知触发者视图对象")
public class NotificationActorVO {

    @Schema(description = "用户 ID")
    private Long userId;

    @Schema(description = "用户名")
    private String username;

    @Schema(description = "头像 URL")
    private String avatar;

    @Schema(description = "是否已注销")
    private Boolean deactivated;
}
