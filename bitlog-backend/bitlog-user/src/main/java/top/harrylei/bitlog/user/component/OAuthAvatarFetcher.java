package top.harrylei.bitlog.user.component;

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
import top.harrylei.bitlog.user.repository.dao.UserInfoDAO;

import java.util.Map;

/**
 * 把第三方头像转存到自己的对象存储
 *
 * @author Harry
 * @since 2026-08-01
 */
@Slf4j
@Component
@RequiredArgsConstructor
public class OAuthAvatarFetcher {

    private static final int CONNECT_TIMEOUT_MS = 5000;
    private static final int READ_TIMEOUT_MS = 10000;

    /** MIME 到扩展名的映射：第三方头像地址通常不带扩展名，只能由响应类型推导 */
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
    private final UserInfoDAO userInfoDAO;

    /**
     * 建号事务提交后再异步转存。
     * <p>
     * 必须等提交：异步线程若先于提交执行，更新的是一条尚不可见的记录，结果是影响 0 行且无任何报错。 整个过程失败一律静默，头像只是锦上添花，不该让登录失败。
     * </p>
     */
    @Async
    @TransactionalEventListener(phase = TransactionPhase.AFTER_COMMIT)
    public void onUserCreated(OAuthAvatarEvent event) {
        if (!StringUtils.hasText(event.avatarUrl())) {
            return;
        }
        try {
            ResponseEntity<byte[]> response =
                REST_CLIENT.get().uri(event.avatarUrl()).retrieve().toEntity(byte[].class);
            byte[] body = response.getBody();
            if (body == null || body.length == 0) {
                return;
            }

            String contentType = contentTypeOf(response.getHeaders());
            String extension = EXTENSIONS.get(contentType);
            if (extension == null) {
                log.warn("第三方头像类型不受支持 userId={} contentType={}", event.userId(), contentType);
                return;
            }

            UploadVO uploaded = fileService.upload(event.userId(), UploadScene.AVATAR, body, contentType, extension);
            userInfoDAO.updateAvatar(event.userId(), uploaded.getFileKey());
            log.info("第三方头像转存成功 userId={} key={}", event.userId(), uploaded.getFileKey());
        } catch (Exception e) {
            log.warn("第三方头像转存失败 userId={}", event.userId(), e);
        }
    }

    private String contentTypeOf(HttpHeaders headers) {
        MediaType mediaType = headers.getContentType();
        // 去掉 charset 等参数，只留 type/subtype 以便匹配白名单
        return mediaType != null ? mediaType.getType() + "/" + mediaType.getSubtype() : null;
    }
}
