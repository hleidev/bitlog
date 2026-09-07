package top.harrylei.bitlog.notification.model.vo;

import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Data;
import lombok.experimental.Accessors;
import top.harrylei.bitlog.notification.model.enums.NotificationTargetTypeEnum;
import top.harrylei.bitlog.notification.model.enums.NotificationTypeEnum;

import java.time.OffsetDateTime;
import java.util.Map;

/**
 * 通知视图对象
 *
 * @author Harry
 * @since 2026-09-07
 */
@Data
@Accessors(chain = true)
@Schema(description = "通知视图对象")
public class NotificationVO {

    @Schema(description = "通知 ID")
    private Long id;

    @Schema(description = "通知类型")
    private NotificationTypeEnum type;

    @Schema(description = "触发者，系统通知为 null")
    private NotificationActorVO actor;

    @Schema(description = "目标对象类型")
    private NotificationTargetTypeEnum targetType;

    @Schema(description = "目标对象 ID")
    private Long targetId;

    @Schema(description = "渲染所需的快照")
    private Map<String, Object> payload;

    @Schema(description = "已读时刻，为 null 表示未读")
    private OffsetDateTime readTime;

    @Schema(description = "发生时间")
    private OffsetDateTime createTime;
}
