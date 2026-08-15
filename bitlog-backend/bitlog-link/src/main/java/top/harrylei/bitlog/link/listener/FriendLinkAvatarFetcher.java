package top.harrylei.bitlog.link.listener;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.http.client.SimpleClientHttpRequestFactory;
import org.springframework.scheduling.annotation.Async;
import org.springframework.stereotype.Component;
import org.springframework.transaction.event.TransactionPhase;
import org.springframework.transaction.event.TransactionalEventListener;
import org.springframework.util.StringUtils;
import org.springframework.web.client.RestClient;
import top.harrylei.bitlog.file.model.UploadScene;
import top.harrylei.bitlog.file.model.UploadVO;
import top.harrylei.bitlog.file.service.FileService;
import top.harrylei.bitlog.link.event.FriendLinkApprovedEvent;
import top.harrylei.bitlog.link.repository.dao.FriendLinkDAO;

import java.util.Map;

/**
 * 把友链头像转存到自己的对象存储
 * <p>
 * 热链对方 CDN 有两个问题：对方一挂本站就破图，且会把访客来源送给对方。
 * </p>
 *
 * @author Harry
 * @since 2026-08-15
 */
@Slf4j
@Component
@RequiredArgsConstructor
public class FriendLinkAvatarFetcher {

    private static final int CONNECT_TIMEOUT_MS = 5000;
    private static final int READ_TIMEOUT_MS = 10000;

    /** MIME 到扩展名的映射：头像地址常常不带扩展名，只能由响应类型推导 */
    private static final Map<String, String> EXTENSIONS =
        Map.of(MediaType.IMAGE_JPEG_VALUE, "jpg", MediaType.IMAGE_PNG_VALUE, "png", "image/webp", "webp");

    private static final RestClient REST_CLIENT = buildRestClient();

    private static RestClient buildRestClient() {
        SimpleClientHttpRequestFactory factory = new SimpleClientHttpRequestFactory();
        factory.setConnectTimeout(CONNECT_TIMEOUT_MS);
        factory.setReadTimeout(READ_TIMEOUT_MS);
        return RestClient.builder().requestFactory(factory).build();
    }

    private final FileService fileService;
    private final FriendLinkDAO friendLinkDAO;

    /**
     * 审核通过的事务提交后再异步转存。
     * <p>
     * 必须等提交：异步线程若先于提交执行，更新的是一条尚不可见的记录，结果是影响 0 行且无任何报错。 失败一律静默并保留原外链——为一张图片让审核操作失败不值得，页面上仍能正常显示对方的图。
     * </p>
     */
    @Async
    @TransactionalEventListener(phase = TransactionPhase.AFTER_COMMIT)
    public void onApproved(FriendLinkApprovedEvent event) {
        if (!StringUtils.hasText(event.avatarUrl())) {
            return;
        }
        try {
            ResponseEntity<byte[]> response = REST_CLIENT.get().uri(event.avatarUrl()).retrieve().toEntity(byte[].class);
            byte[] body = response.getBody();
            if (body == null || body.length == 0) {
                return;
            }

            String contentType = contentTypeOf(response.getHeaders());
            String extension = EXTENSIONS.get(contentType);
            if (extension == null) {
                log.warn("友链头像类型不受支持 linkId={} contentType={}", event.linkId(), contentType);
                return;
            }

            UploadVO uploaded = fileService.upload(event.operatorId(), UploadScene.AVATAR, body, contentType, extension);
            friendLinkDAO.updateAvatar(event.linkId(), uploaded.getFileKey());
            log.info("友链头像转存成功 linkId={} key={}", event.linkId(), uploaded.getFileKey());
        } catch (Exception e) {
            log.warn("友链头像转存失败 linkId={} url={}", event.linkId(), event.avatarUrl(), e);
        }
    }

    private String contentTypeOf(HttpHeaders headers) {
        MediaType mediaType = headers.getContentType();
        // 去掉 charset 等参数，只留 type/subtype 以便匹配白名单
        return mediaType != null ? mediaType.getType() + "/" + mediaType.getSubtype() : null;
    }
}
