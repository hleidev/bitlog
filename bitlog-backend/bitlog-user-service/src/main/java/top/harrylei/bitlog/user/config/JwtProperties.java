package top.harrylei.bitlog.user.config;

import lombok.Data;
import org.springframework.boot.context.properties.ConfigurationProperties;

import java.time.Duration;

/**
 * JWT 配置参数
 *
 * @author harry
 * @since 0.0.1
 */
@Data
@ConfigurationProperties(prefix = "jwt")
public class JwtProperties {

    /**
     * 颁发者
     */
    private String issuer;

    /**
     * 签名密钥
     */
    private String secret;

    /**
     * 默认有效期
     */
    private Duration defaultExpire;

    /**
     * 保持登录有效期
     */
    private Duration keepLoginExpire;
}
