package top.harrylei.bitlog.auth.config;

import lombok.Data;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.validation.annotation.Validated;
import top.harrylei.bitlog.common.config.EnvInjected;

/**
 * 只为校验而存在：Google 的 client-secret 绑在 Spring 自己的 OAuth2ClientProperties 上，
 * 那个类不拒绝未解析的 ${VAR} 字面量，缺变量要拖到用户点击 Google 登录、换 token 被拒时才暴露。
 * 这里用同一前缀再绑一次，把失败提前到启动。不被任何组件注入，登录流程仍走 Spring 的注册表。
 *
 * @author Harry
 * @since 2026-08-18
 */
@Data
@Validated
@ConfigurationProperties(prefix = "spring.security.oauth2.client.registration.google")
public class GoogleClientRegistrationProperties {

    @EnvInjected
    private String clientSecret;
}
