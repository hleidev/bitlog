package top.harrylei.bitlog.link.listener;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.context.event.EventListener;
import org.springframework.stereotype.Component;
import top.harrylei.bitlog.link.repository.dao.FriendLinkDAO;
import top.harrylei.bitlog.user.event.UserDeactivatedEvent;

/**
 * 账号注销后解除其友链的归属
 *
 * @author Harry
 * @since 2026-08-15
 */
@Slf4j
@Component
@RequiredArgsConstructor
public class FriendLinkOwnerCleanupListener {

    private final FriendLinkDAO friendLinkDAO;

    /**
     * 友链本身保留，只把归属抹掉，降级为站长托管。
     * <p>
     * 友链是站点之间的关系，不是账号的附属物——人注销了，站还在，链接也该还在，只是不再有人能自助管理。
     * </p>
     * <p>
     * 用同步 {@link EventListener} 而非 {@code @TransactionalEventListener}：清理必须留在发布方的事务里， 否则注销一旦回滚，友链的归属已经被抹掉且无从恢复。这与
     * AccountCleanupListener 的取舍一致。
     * </p>
     */
    @EventListener
    public void onUserDeactivated(UserDeactivatedEvent event) {
        if (friendLinkDAO.detachOwner(event.userId())) {
            log.info("账号注销，友链降级为站长托管 userId={}", event.userId());
        }
    }
}
