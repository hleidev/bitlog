package top.harrylei.bitlog.user.config;

import lombok.Data;
import org.springframework.boot.context.properties.ConfigurationProperties;

/**
 * Cookie 配置参数
 *
 * @author harry
 * @since 0.0.1
 */
@Data
@ConfigurationProperties(prefix = "cookie")
public class CookieProperties {

    /**
     * 是否只在 HTTPS 下发送（生产环境设为 true）
     */
    private boolean secure = true;
}
