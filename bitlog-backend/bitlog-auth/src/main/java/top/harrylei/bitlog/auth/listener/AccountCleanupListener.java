package top.harrylei.bitlog.auth.listener;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.context.event.EventListener;
import org.springframework.stereotype.Component;
import top.harrylei.bitlog.auth.support.RefreshTokenStore;
import top.harrylei.bitlog.user.event.UserDeactivatedEvent;
import top.harrylei.bitlog.user.event.UserDisabledEvent;
import top.harrylei.bitlog.auth.repository.dao.UserIdentityDAO;

/**
 * 账号生命周期变更时的认证侧清理
 * <p>
 * 刻意用同步 {@link EventListener} 而非 {@code @TransactionalEventListener}：清理必须与发布方 处在同一事务内，注销回滚时不能留下已被解绑的第三方身份。
 * </p>
 *
 * @author Harry
 * @since 2026-08-09
 */
@Slf4j
@Component
@RequiredArgsConstructor
public class AccountCleanupListener {

    private final UserIdentityDAO userIdentityDAO;
    private final RefreshTokenStore refreshTokenStore;

    @EventListener
    public void onUserDeactivated(UserDeactivatedEvent event) {
        Long userId = event.userId();
        // 物理删除：不删则 uk_provider_uid 仍占位，同一第三方账号将永远无法重新注册
        userIdentityDAO.removeByUserId(userId);
        refreshTokenStore.revokeAll(userId, null);
        log.info("注销清理完成 userId={}", userId);
    }

    @EventListener
    public void onUserDisabled(UserDisabledEvent event) {
        event.userIds().forEach(userId -> refreshTokenStore.revokeAll(userId, null));
        log.info("封禁撤销会话 userIds={}", event.userIds());
    }
}
