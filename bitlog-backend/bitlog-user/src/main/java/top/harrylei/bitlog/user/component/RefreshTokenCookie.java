package top.harrylei.bitlog.user.component;

import jakarta.servlet.http.HttpServletResponse;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpHeaders;
import org.springframework.http.ResponseCookie;
import org.springframework.stereotype.Component;
import top.harrylei.bitlog.common.config.JwtProperties;
import top.harrylei.bitlog.user.config.CookieProperties;

/**
 * Refresh Token 的 Cookie 读写，供密码登录与第三方登录共用
 *
 * @author Harry
 * @since 2026-07-31
 */
@Component
@RequiredArgsConstructor
public class RefreshTokenCookie {

    public static final String COOKIE_NAME = "refresh_token";

    private static final String COOKIE_PATH = "/";

    private final JwtProperties jwtProperties;
    private final CookieProperties cookieProperties;

    public void write(HttpServletResponse response, String refreshToken) {
        ResponseCookie cookie = baseCookie(refreshToken).maxAge(jwtProperties.getRefreshTokenExpire()).build();
        response.addHeader(HttpHeaders.SET_COOKIE, cookie.toString());
    }

    public void clear(HttpServletResponse response) {
        response.addHeader(HttpHeaders.SET_COOKIE, baseCookie("").maxAge(0).build().toString());
    }

    private ResponseCookie.ResponseCookieBuilder baseCookie(String value) {
        // SameSite=Lax 承担 CSRF 防护职责：/auth/refresh 与 /auth/logout 以本 Cookie 为唯一凭据，
        // 而 SecurityConfig 已禁用 CSRF token，全靠 Lax 拦住跨站 POST。
        // 前端若改为跨站部署（与后端不同注册域），放宽为 None 会让这两个接口直接暴露，
        // 届时需改走同源代理或为它们补上 CSRF 校验
        return ResponseCookie.from(COOKIE_NAME, value).httpOnly(true).secure(cookieProperties.isSecure())
            .sameSite("Lax").path(COOKIE_PATH);
    }
}
