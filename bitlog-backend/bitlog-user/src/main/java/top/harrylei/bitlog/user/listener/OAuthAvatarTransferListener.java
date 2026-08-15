package top.harrylei.bitlog.user.listener;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.scheduling.annotation.Async;
import org.springframework.stereotype.Component;
import org.springframework.transaction.event.TransactionPhase;
import org.springframework.transaction.event.TransactionalEventListener;
import top.harrylei.bitlog.file.model.UploadScene;
import top.harrylei.bitlog.file.model.UploadVO;
import top.harrylei.bitlog.file.util.RemoteImageFetcher;
import top.harrylei.bitlog.user.event.OAuthAvatarEvent;
import top.harrylei.bitlog.user.repository.dao.UserInfoDAO;

/**
 * 把第三方头像转存到自己的对象存储
 *
 * @author Harry
 * @since 2026-08-01
 */
@Slf4j
@Component
@RequiredArgsConstructor
public class OAuthAvatarTransferListener {

    private final RemoteImageFetcher remoteImageFetcher;
    private final UserInfoDAO userInfoDAO;

    /**
     * 建号事务提交后再异步转存。
     * <p>
     * 必须等提交：异步线程若先于提交执行，更新的是一条尚不可见的记录，结果是影响 0 行且无任何报错。 抓取失败一律静默，头像只是锦上添花，不该让登录失败。
     * </p>
     */
    @Async
    @TransactionalEventListener(phase = TransactionPhase.AFTER_COMMIT)
    public void onUserCreated(OAuthAvatarEvent event) {
        UploadVO uploaded = remoteImageFetcher.fetchAndStore(event.avatarUrl(), event.userId(), UploadScene.AVATAR);
        if (uploaded == null) {
            return;
        }
        userInfoDAO.updateAvatar(event.userId(), uploaded.getFileKey());
        log.info("第三方头像转存成功 userId={} key={}", event.userId(), uploaded.getFileKey());
    }
}
