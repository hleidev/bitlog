package top.harrylei.bitlog.common.util;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;
import org.springframework.util.StringUtils;

/**
 * 文件 URL 构建工具
 * <p>DB 只存储文件 key，对外返回时通过本类拼接公共访问前缀。</p>
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
     * @param key
     *            文件存储路径（如 bitlog/avatar/1/2026/04/xxx.jpeg）
     * 
     * @return 完整 URL，key 为空时原样返回
     */
    public String buildUrl(String key) {
        if (!StringUtils.hasText(key)) {
            return key;
        }
        return publicUrl + "/" + key;
    }
}
