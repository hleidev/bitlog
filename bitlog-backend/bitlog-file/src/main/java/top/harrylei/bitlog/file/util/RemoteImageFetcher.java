package top.harrylei.bitlog.file.util;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.http.client.SimpleClientHttpRequestFactory;
import org.springframework.stereotype.Component;
import org.springframework.util.StringUtils;
import org.springframework.web.client.RestClient;
import top.harrylei.bitlog.file.model.UploadScene;
import top.harrylei.bitlog.file.model.UploadVO;
import top.harrylei.bitlog.file.service.FileService;

import java.time.Duration;
import java.util.Map;

/**
 * 抓取远端图片并转存到自有对象存储
 * <p>
 * 热链他站资源有两个问题：对方一挂本站就破图，且会把访客来源送给对方。 第三方登录头像与友链头像都要做同一件事，抓取与类型判定收在这里，各自只保留回写。
 * </p>
 *
 * @author Harry
 * @since 2026-08-15
 */
@Slf4j
@Component
@RequiredArgsConstructor
public class RemoteImageFetcher {

    private static final Duration CONNECT_TIMEOUT = Duration.ofSeconds(5);
    private static final Duration READ_TIMEOUT = Duration.ofSeconds(10);

    /** MIME 到扩展名的映射：图片地址常常不带扩展名，只能由响应类型推导 */
    private static final Map<String, String> EXTENSIONS =
        Map.of(MediaType.IMAGE_JPEG_VALUE, "jpg", MediaType.IMAGE_PNG_VALUE, "png", "image/webp", "webp");

    private static final RestClient REST_CLIENT = buildRestClient();

    private static RestClient buildRestClient() {
        SimpleClientHttpRequestFactory factory = new SimpleClientHttpRequestFactory();
        factory.setConnectTimeout(CONNECT_TIMEOUT);
        factory.setReadTimeout(READ_TIMEOUT);
        return RestClient.builder().requestFactory(factory).build();
    }

    private final FileService fileService;

    /**
     * 抓取远端图片并转存
     * <p>
     * 失败一律静默：图片是锦上添花，不值得让调用方的主流程跟着失败，保留原外链即可。
     * </p>
     *
     * @param url 远端图片地址，为空时直接跳过
     * @param ownerId 转存后文件的归属用户，决定对象存储的 key 路径
     * @param scene 上传场景
     * @return 转存结果；地址为空、类型不受支持或抓取失败时返回 null
     */
    public UploadVO fetchAndStore(String url, Long ownerId, UploadScene scene) {
        if (!StringUtils.hasText(url)) {
            return null;
        }
        try {
            ResponseEntity<byte[]> response = REST_CLIENT.get().uri(url).retrieve().toEntity(byte[].class);
            byte[] body = response.getBody();
            if (body == null || body.length == 0) {
                return null;
            }

            String contentType = contentTypeOf(response.getHeaders());
            String extension = EXTENSIONS.get(contentType);
            if (extension == null) {
                log.warn("远端图片类型不受支持 url={} contentType={}", url, contentType);
                return null;
            }

            return fileService.upload(ownerId, scene, body, contentType, extension);
        } catch (Exception e) {
            log.warn("远端图片转存失败 url={}", url, e);
            return null;
        }
    }

    private String contentTypeOf(HttpHeaders headers) {
        MediaType mediaType = headers.getContentType();
        // 去掉 charset 等参数，只留 type/subtype 以便匹配白名单
        return mediaType != null ? mediaType.getType() + "/" + mediaType.getSubtype() : null;
    }
}
