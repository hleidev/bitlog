package top.harrylei.bitlog.notification.task;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;
import top.harrylei.bitlog.notification.repository.dao.NotificationDAO;

/**
 * 过期通知清理定时任务
 *
 * @author Harry
 * @since 2026-09-07
 */
@Slf4j
@Component
@RequiredArgsConstructor
public class NotificationCleanupTask {

    private static final int RETENTION_DAYS = 90;

    private final NotificationDAO notificationDAO;

    @Scheduled(cron = "0 30 3 * * *")
    public void cleanExpiredNotifications() {
        int cleaned = notificationDAO.removeExpired(RETENTION_DAYS);
        if (cleaned > 0) {
            log.info("清理过期通知 {} 条", cleaned);
        }
    }
}
