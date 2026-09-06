package top.harrylei.bitlog.notification.model.dto;

import top.harrylei.bitlog.notification.model.enums.NotificationTargetTypeEnum;
import top.harrylei.bitlog.notification.model.enums.NotificationTypeEnum;

import java.util.Map;

/**
 * 一条候选通知，由业务模块在事件发生时构造，交给 NotificationPort#dispatch 批量派发
 *
 * @param recipientId 收件人，关联 user_account.id
 * @param type 通知类型
 * @param actorId 触发者，关联 user_account.id；系统通知为 null
 * @param targetType 目标对象类型
 * @param targetId 目标对象 ID，随 targetType 指向不同的表
 * @param payload 渲染所需的快照
 * @author Harry
 * @since 2026-09-06
 */
public record NotificationCreateDTO(Long recipientId, NotificationTypeEnum type, Long actorId,
    NotificationTargetTypeEnum targetType, Long targetId, Map<String, Object> payload) {
}
