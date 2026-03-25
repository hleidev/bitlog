package top.harrylei.community.gateway.config;

import lombok.Data;
import org.springframework.boot.context.properties.ConfigurationProperties;

/**
 * JWT 配置（与 user-service 保持一致）
 *
 * @author harry
 * @since 0.0.1
 */
@Data
@ConfigurationProperties(prefix = "jwt")
public class JwtProperties {

    /** 签名密钥，必须与 user-service 一致 */
    private String secret;

    /** 签发方 */
    private String issuer;
}
