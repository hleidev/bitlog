package top.harrylei.bitlog.file.util;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;
import org.springframework.util.StringUtils;

/**
 * 文件 URL 工具：key ↔ 完整 URL 互转
 *
 * @author Harry
 * @since 2026-04-25
 */
@Component
public class FileUrlHelper {

    @Value("${storage.public-url:http://localhost:8001/files}")
    private String publicUrl;

    /**
     * 将文件 key 拼接为完整访问 URL
     *
     * @param key 文件存储路径（如 bitlog/avatar/1/2026/04/xxx.jpeg）
     * @return 完整 URL，key 为空时原样返回
     */
    public String buildUrl(String key) {
        if (!StringUtils.hasText(key)) {
            return key;
        }
        return publicUrl + "/" + key;
    }

    /**
     * 从完整 URL 中提取文件 key；若传入的已是 key 则原样返回
     *
     * @param urlOrKey 完整 URL 或文件 key
     * @return 文件 key
     */
    public String extractKey(String urlOrKey) {
        if (!StringUtils.hasText(urlOrKey)) {
            return urlOrKey;
        }
        String prefix = publicUrl + "/";
        if (urlOrKey.startsWith(prefix)) {
            return urlOrKey.substring(prefix.length());
        }
        return urlOrKey;
    }
}
