package top.harrylei.bitlog.user.config;

import lombok.Data;
import org.springframework.boot.context.properties.ConfigurationProperties;

/**
 * 第三方登录完成后的前端跳转配置
 *
 * @author Harry
 * @since 2026-07-31
 */
@Data
@ConfigurationProperties(prefix = "oauth2")
public class OAuth2Properties {

    /** 授权结束后回跳的前端地址，成功与失败共用，失败时追加 error 参数 */
    private String redirectUri;
}
