package top.harrylei.bitlog.link.listener;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.scheduling.annotation.Async;
import org.springframework.stereotype.Component;
import org.springframework.transaction.event.TransactionPhase;
import org.springframework.transaction.event.TransactionalEventListener;
import top.harrylei.bitlog.file.model.UploadScene;
import top.harrylei.bitlog.file.model.UploadVO;
import top.harrylei.bitlog.file.util.RemoteImageFetcher;
import top.harrylei.bitlog.link.event.FriendLinkApprovedEvent;
import top.harrylei.bitlog.link.repository.dao.FriendLinkDAO;

/**
 * 把友链头像转存到自己的对象存储
 *
 * @author Harry
 * @since 2026-08-15
 */
@Slf4j
@Component
@RequiredArgsConstructor
public class FriendLinkAvatarTransferListener {

    private final RemoteImageFetcher remoteImageFetcher;
    private final FriendLinkDAO friendLinkDAO;

    /**
     * 友链进入展示状态的事务提交后再异步转存。
     * <p>
     * 必须等提交：异步线程若先于提交执行，更新的是一条尚不可见的记录，结果是影响 0 行且无任何报错。 抓取失败一律静默并保留原外链——为一张图片让审核操作失败不值得，页面上仍能正常显示对方的图。
     * </p>
     */
    @Async
    @TransactionalEventListener(phase = TransactionPhase.AFTER_COMMIT)
    public void onApproved(FriendLinkApprovedEvent event) {
        UploadVO uploaded = remoteImageFetcher.fetchAndStore(event.avatarUrl(), event.operatorId(), UploadScene.AVATAR);
        if (uploaded == null) {
            return;
        }
        friendLinkDAO.updateAvatar(event.linkId(), uploaded.getFileKey());
        log.info("友链头像转存成功 linkId={} key={}", event.linkId(), uploaded.getFileKey());
    }
}
