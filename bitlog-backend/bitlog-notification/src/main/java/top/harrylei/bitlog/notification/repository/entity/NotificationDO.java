package top.harrylei.bitlog.notification.repository.entity;

import com.baomidou.mybatisplus.annotation.TableField;
import com.baomidou.mybatisplus.annotation.TableName;
import lombok.Data;
import lombok.EqualsAndHashCode;
import lombok.experimental.Accessors;
import top.harrylei.bitlog.common.enums.DeleteStatusEnum;
import top.harrylei.bitlog.common.handler.JsonbTypeHandler;
import top.harrylei.bitlog.common.model.BaseDO;
import top.harrylei.bitlog.notification.model.enums.NotificationTargetTypeEnum;
import top.harrylei.bitlog.notification.model.enums.NotificationTypeEnum;

import java.io.Serial;
import java.time.OffsetDateTime;
import java.util.Map;

/**
 * 通知实体
 *
 * @author Harry
 * @since 2026-09-06
 */
@Data
@EqualsAndHashCode(callSuper = true)
@Accessors(chain = true)
@TableName(value = "notification", autoResultMap = true)
public class NotificationDO extends BaseDO {

    @Serial
    private static final long serialVersionUID = 1L;

    /**
     * 收件人，关联 user_account.id
     */
    private Long recipientId;

    /**
     * 通知类型
     */
    private NotificationTypeEnum type;

    /**
     * 触发者，关联 user_account.id；系统通知为 NULL
     */
    private Long actorId;

    /**
     * 目标对象类型
     */
    private NotificationTargetTypeEnum targetType;

    /**
     * 目标对象 ID，随 targetType 指向不同的表；多态引用，不建外键
     */
    private Long targetId;

    /**
     * 渲染所需的快照
     */
    // 异构 JSON 快照，值类型无法在类型系统上证明可序列化；DO 不走 Java 序列化，故压制 S1948
    @SuppressWarnings("java:S1948")
    @TableField(typeHandler = JsonbTypeHandler.class)
    private Map<String, Object> payload;

    /**
     * 已读时刻，NULL 即未读
     */
    private OffsetDateTime readTime;

    /**
     * 是否删除：0-未删除，1-已删除
     */
    private DeleteStatusEnum deleted;
}
