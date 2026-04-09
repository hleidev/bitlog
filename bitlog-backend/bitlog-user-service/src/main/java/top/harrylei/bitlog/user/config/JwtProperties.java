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
     * Access Token 有效期（默认 15 分钟）
     */
    private Duration accessTokenExpire = Duration.ofMinutes(15);

    /**
     * Refresh Token 有效期（默认 30 天）
     */
    private Duration refreshTokenExpire = Duration.ofDays(30);
}
