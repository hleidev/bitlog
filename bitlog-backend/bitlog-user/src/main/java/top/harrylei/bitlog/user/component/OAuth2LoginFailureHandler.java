package top.harrylei.bitlog.user.component;

import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.security.core.AuthenticationException;
import org.springframework.stereotype.Component;
import top.harrylei.bitlog.common.security.OAuth2FailureHandler;
import top.harrylei.bitlog.user.config.OAuth2Properties;

import java.io.IOException;

/**
 * 第三方授权失败后回跳前端并带上错误标识
 *
 * @author Harry
 * @since 2026-07-31
 */
@Slf4j
@Component
@RequiredArgsConstructor
public class OAuth2LoginFailureHandler implements OAuth2FailureHandler {

    private final OAuth2Properties oAuth2Properties;

    @Override
    public void onAuthenticationFailure(HttpServletRequest request, HttpServletResponse response,
        AuthenticationException exception) throws IOException {
        // 用户在授权页点了取消也会走到这里，属于正常流程，不记 ERROR
        log.warn("第三方授权失败: {}", exception.getMessage());
        response.sendRedirect(oAuth2Properties.getRedirectUri() + "?error=oauth_failed");
    }
}
