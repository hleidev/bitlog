package top.harrylei.bitlog.notification.listener;

import lombok.RequiredArgsConstructor;
import org.springframework.context.event.EventListener;
import org.springframework.stereotype.Component;
import top.harrylei.bitlog.link.event.FriendLinkAppliedEvent;
import top.harrylei.bitlog.link.event.FriendLinkReviewedEvent;
import top.harrylei.bitlog.notification.model.dto.NotificationCreateDTO;
import top.harrylei.bitlog.notification.model.enums.NotificationTargetTypeEnum;
import top.harrylei.bitlog.notification.model.enums.NotificationTypeEnum;
import top.harrylei.bitlog.notification.port.NotificationPort;
import top.harrylei.bitlog.user.port.UserPort;

import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * 友链申请与审核完成后按规则派发通知
 *
 * @author Harry
 * @since 2026-09-07
 */
@Component
@RequiredArgsConstructor
public class FriendLinkNotificationListener {

    private static final String APPLIED_DEDUPE_KEY_PREFIX = "link_applied:";

    private final NotificationPort notificationPort;
    private final UserPort userPort;

    /**
     * 同步监听友链申请事件并通知管理员
     *
     * @param event 友链申请事件
     */
    @EventListener
    public void onFriendLinkApplied(FriendLinkAppliedEvent event) {
        List<Long> adminIds = userPort.listAdminIds();
        if (adminIds.isEmpty()) {
            return;
        }

        Map<String, Object> payload = new HashMap<>();
        payload.put("linkName", event.linkName());
        payload.put("linkUrl", event.linkUrl());
        payload.put("applyMessage", event.applyMessage());
        String dedupeKey = APPLIED_DEDUPE_KEY_PREFIX + event.linkId();
        List<NotificationCreateDTO> candidates = adminIds.stream()
                .map(adminId -> new NotificationCreateDTO(
                        adminId,
                        NotificationTypeEnum.LINK_APPLIED,
                        event.applicantId(),
                        NotificationTargetTypeEnum.FRIEND_LINK,
                        event.linkId(),
                        payload,
                        dedupeKey))
                .toList();
        notificationPort.dispatch(candidates);
    }

    /**
     * 同步监听友链审核事件并通知申请人
     *
     * @param event 友链审核事件
     */
    @EventListener
    public void onFriendLinkReviewed(FriendLinkReviewedEvent event) {
        if (event.ownerId() == null) {
            return;
        }

        Map<String, Object> payload = new HashMap<>();
        payload.put("linkName", event.linkName());
        payload.put("status", event.status().getCode());
        payload.put("rejectReason", event.rejectReason());
        // 审核通知不参与去重
        NotificationCreateDTO candidate = new NotificationCreateDTO(
                event.ownerId(),
                NotificationTypeEnum.LINK_REVIEWED,
                event.operatorId(),
                NotificationTargetTypeEnum.FRIEND_LINK,
                event.linkId(),
                payload,
            null);
        notificationPort.dispatch(List.of(candidate));
    }
}
