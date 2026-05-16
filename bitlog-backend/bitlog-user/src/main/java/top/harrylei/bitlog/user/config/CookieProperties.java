package top.harrylei.bitlog.user.config;

import lombok.Data;
import org.springframework.boot.context.properties.ConfigurationProperties;

/**
 * Cookie 配置参数
 *
 * @author Harry
 * @since 2026-04-09
 */
@Data
@ConfigurationProperties(prefix = "cookie")
public class CookieProperties {

    private boolean secure = true;
}
