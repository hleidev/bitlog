package top.harrylei.bitlog.user.component;

import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.security.core.Authentication;
import org.springframework.security.oauth2.client.authentication.OAuth2AuthenticationToken;
import org.springframework.security.oauth2.core.endpoint.OAuth2ParameterNames;
import org.springframework.security.oauth2.core.oidc.user.OidcUser;
import org.springframework.stereotype.Component;
import org.springframework.util.StringUtils;
import top.harrylei.bitlog.api.model.auth.OAuthLoginParam;
import top.harrylei.bitlog.common.exception.BusinessException;
import top.harrylei.bitlog.common.security.OAuth2SuccessHandler;
import top.harrylei.bitlog.common.util.MaskUtil;
import top.harrylei.bitlog.user.config.OAuth2Properties;
import top.harrylei.bitlog.user.service.AuthService;
import top.harrylei.bitlog.user.service.LoginResult;

import java.io.IOException;
import java.net.URLEncoder;
import java.nio.charset.StandardCharsets;

/**
 * 第三方授权成功后建号或登录，并把会话交还给前端
 *
 * @author Harry
 * @since 2026-07-31
 */
@Slf4j
@Component
@RequiredArgsConstructor
public class OAuth2LoginSuccessHandler implements OAuth2SuccessHandler {

    private static final String ERROR_UNSUPPORTED = "unsupported_provider";
    private static final String ERROR_REJECTED = "login_rejected";
    private static final String ERROR_SERVER = "server_error";
    private static final String BIND_SUCCESS = "success";
    private static final String BIND_FAILED = "failed";

    private final AuthService authService;
    private final RefreshTokenCookie refreshTokenCookie;
    private final OAuth2Properties oAuth2Properties;

    @Override
    public void onAuthenticationSuccess(HttpServletRequest request, HttpServletResponse response,
        Authentication authentication) throws IOException {
        if (!(authentication instanceof OAuth2AuthenticationToken token)
            || !(token.getPrincipal()instanceof OidcUser oidcUser)) {
            redirectWithError(response, ERROR_UNSUPPORTED);
            return;
        }

        OAuthLoginParam param = new OAuthLoginParam().setProvider(token.getAuthorizedClientRegistrationId())
            .setProviderUserId(oidcUser.getSubject()).setEmail(oidcUser.getEmail())
            .setEmailVerified(Boolean.TRUE.equals(oidcUser.getEmailVerified())).setName(oidcUser.getFullName())
            .setAvatarUrl(oidcUser.getPicture());

        // 授权入口若带了绑定意图，本次是往已登录账号上挂身份，而非登录或建号
        String intentToken = extractBindIntent(request.getParameter(OAuth2ParameterNames.STATE));
        if (intentToken != null) {
            handleBind(response, intentToken, param);
            return;
        }

        // 这里位于过滤器链上，GlobalExceptionHandler 覆盖不到；不兜住异常，
        // 用户在浏览器导航途中看到的会是容器的 500 错误页而非回跳后的提示
        LoginResult result;
        try {
            result = authService.loginWithOAuth(param);
        } catch (BusinessException e) {
            log.warn("第三方登录被拒 provider={} code={} message={}", param.getProvider(), e.getCode(), e.getMessage());
            redirectWithError(response, ERROR_REJECTED);
            return;
        } catch (RuntimeException e) {
            log.error("第三方登录处理异常 provider={}", param.getProvider(), e);
            redirectWithError(response, ERROR_SERVER);
            return;
        }

        refreshTokenCookie.write(response, result.refreshToken());
        log.info("第三方登录成功 provider={} email={}", param.getProvider(), MaskUtil.email(param.getEmail()));
        // Access Token 不放进 URL，前端回跳后用 Cookie 里的 Refresh Token 换取，
        // 与页面刷新恢复会话走同一条路径
        response.sendRedirect(oAuth2Properties.getRedirectUri());
    }

    private void handleBind(HttpServletResponse response, String intentToken, OAuthLoginParam param)
        throws IOException {
        try {
            authService.bindWithOAuth(intentToken, param);
        } catch (BusinessException e) {
            log.warn("第三方绑定被拒 provider={} code={} message={}", param.getProvider(), e.getCode(), e.getMessage());
            redirectAfterBind(response, BIND_FAILED, e.getMessage());
            return;
        } catch (RuntimeException e) {
            log.error("第三方绑定处理异常 provider={}", param.getProvider(), e);
            redirectAfterBind(response, BIND_FAILED, null);
            return;
        }

        log.info("第三方账号绑定成功 provider={} email={}", param.getProvider(), MaskUtil.email(param.getEmail()));
        redirectAfterBind(response, BIND_SUCCESS, null);
    }

    /**
     * 从 state 中切出绑定意图令牌，没有分隔符即为普通登录
     */
    private String extractBindIntent(String state) {
        if (!StringUtils.hasText(state)) {
            return null;
        }
        int index = state.indexOf(STATE_INTENT_SEPARATOR);
        return index > 0 && index < state.length() - 1 ? state.substring(index + 1) : null;
    }

    private void redirectAfterBind(HttpServletResponse response, String status, String reason) throws IOException {
        StringBuilder target = new StringBuilder(oAuth2Properties.getBindRedirectUri()).append("?bind=").append(status);
        if (StringUtils.hasText(reason)) {
            target.append("&reason=").append(URLEncoder.encode(reason, StandardCharsets.UTF_8));
        }
        response.sendRedirect(target.toString());
    }

    private void redirectWithError(HttpServletResponse response, String error) throws IOException {
        response.sendRedirect(oAuth2Properties.getRedirectUri() + "?error=" + error);
    }
}
