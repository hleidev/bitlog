package top.harrylei.bitlog.common.security;

import org.springframework.security.web.authentication.AuthenticationFailureHandler;

/**
 * 第三方授权失败处理器，立类型的理由同 {@link OAuth2SuccessHandler}
 *
 * @author Harry
 * @since 2026-07-31
 */
public interface OAuth2FailureHandler extends AuthenticationFailureHandler {}
