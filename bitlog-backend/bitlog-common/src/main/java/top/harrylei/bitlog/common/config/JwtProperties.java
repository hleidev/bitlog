package top.harrylei.bitlog.common.config;

import lombok.Data;
import org.springframework.boot.context.properties.ConfigurationProperties;

import java.time.Duration;

@Data
@ConfigurationProperties(prefix = "jwt")
public class JwtProperties {
    private String issuer;
    private String secret;
    private Duration accessTokenExpire = Duration.ofMinutes(15);
    private Duration refreshTokenExpire = Duration.ofDays(30);
}
