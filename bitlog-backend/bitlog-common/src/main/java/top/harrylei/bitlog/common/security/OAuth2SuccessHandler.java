package top.harrylei.bitlog.common.security;

import org.springframework.security.web.authentication.AuthenticationSuccessHandler;

/**
 * 第三方授权成功处理器。
 * <p>
 * 单独立类型是为了让 SecurityConfig 能按类型精确注入：直接注入 {@link AuthenticationSuccessHandler} 在容器里可能匹配到多个候选， 依赖 {@code @Qualifier} 按
 * bean 名解析则把配置正确性寄托在命名约定上。
 * </p>
 *
 * @author Harry
 * @since 2026-07-31
 */
public interface OAuth2SuccessHandler extends AuthenticationSuccessHandler {

    /**
     * state 中随机段与绑定意图令牌的分隔符。 state 的随机段走 Base64 URL 编码，字符集为 A-Za-z0-9-_= ，不含 '.'，故不会与令牌混淆。
     */
    String STATE_INTENT_SEPARATOR = ".";
}
