package top.harrylei.bitlog.notification.listener;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.context.event.EventListener;
import org.springframework.stereotype.Component;
import top.harrylei.bitlog.notification.repository.dao.NotificationDAO;
import top.harrylei.bitlog.user.event.UserDeactivatedEvent;

/**
 * 账号注销后的收件通知清理器
 *
 * @author Harry
 * @since 2026-09-07
 */
@Slf4j
@Component
@RequiredArgsConstructor
public class DeactivatedRecipientCleanupListener {

    private final NotificationDAO notificationDAO;

    @EventListener
    public void onUserDeactivated(UserDeactivatedEvent event) {
        // 仅清理收件通知，作为触发者产生的通知继续保留
        int cleaned = notificationDAO.markAllDeleted(event.userId());
        if (cleaned > 0) {
            log.info("账号注销，清理收件通知 userId={} count={}", event.userId(), cleaned);
        }
    }
}
