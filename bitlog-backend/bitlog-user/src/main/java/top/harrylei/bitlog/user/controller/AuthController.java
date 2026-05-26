package top.harrylei.bitlog.user.controller;

import jakarta.servlet.http.HttpServletResponse;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpHeaders;
import org.springframework.http.ResponseCookie;
import org.springframework.util.StringUtils;
import org.springframework.web.bind.annotation.*;
import top.harrylei.bitlog.api.enums.user.UserRoleEnum;
import top.harrylei.bitlog.api.model.auth.LoginParam;
import top.harrylei.bitlog.api.model.auth.LoginVO;
import top.harrylei.bitlog.api.model.auth.UserCreateParam;
import top.harrylei.bitlog.common.enums.ResultCode;
import top.harrylei.bitlog.common.model.Result;
import top.harrylei.bitlog.common.security.RequiresAdmin;
import top.harrylei.bitlog.user.config.CookieProperties;
import top.harrylei.bitlog.common.config.JwtProperties;
import top.harrylei.bitlog.user.service.AuthService;
import top.harrylei.bitlog.user.service.LoginResult;

/**
 * 认证接口
 *
 * @author Harry
 * @since 2026-03-21
 */
@RestController
@RequestMapping("/api/v1/auth")
@RequiredArgsConstructor
public class AuthController {

    private static final String REFRESH_TOKEN_COOKIE = "refresh_token";
    private static final String COOKIE_PATH = "/";

    private final AuthService authService;
    private final JwtProperties jwtProperties;
    private final CookieProperties cookieProperties;

    @PostMapping("/register")
    public Result<Void> register(@Valid @RequestBody LoginParam request) {
        authService.register(request.getUsername(), request.getPassword(), UserRoleEnum.NORMAL);
        return Result.success();
    }

    @PostMapping("/login")
    public Result<LoginVO> login(@Valid @RequestBody LoginParam request, HttpServletResponse response) {
        LoginResult result = authService.login(request.getUsername(), request.getPassword());
        setRefreshTokenCookie(response, result.refreshToken());
        return Result.success(new LoginVO(result.accessToken()));
    }

    @PostMapping("/refresh")
    public Result<LoginVO> refresh(@CookieValue(name = REFRESH_TOKEN_COOKIE, required = false) String refreshToken,
        HttpServletResponse response) {
        if (!StringUtils.hasText(refreshToken)) {
            return Result.fail(ResultCode.REFRESH_TOKEN_INVALID);
        }
        LoginResult result = authService.refresh(refreshToken);
        setRefreshTokenCookie(response, result.refreshToken());
        return Result.success(new LoginVO(result.accessToken()));
    }

    @PostMapping("/logout")
    public Result<Void> logout(@CookieValue(name = REFRESH_TOKEN_COOKIE, required = false) String refreshToken,
        HttpServletResponse response) {
        authService.logout(refreshToken);
        clearRefreshTokenCookie(response);
        return Result.success();
    }

    @RequiresAdmin
    @PostMapping("/admin/create")
    public Result<Void> createUser(@Valid @RequestBody UserCreateParam request) {
        authService.register(request.getUsername(), request.getPassword(), request.getRole());
        return Result.success();
    }

    private void setRefreshTokenCookie(HttpServletResponse response, String refreshToken) {
        ResponseCookie cookie =
            ResponseCookie.from(REFRESH_TOKEN_COOKIE, refreshToken).httpOnly(true).secure(cookieProperties.isSecure())
                .sameSite("Lax").path(COOKIE_PATH).maxAge(jwtProperties.getRefreshTokenExpire()).build();
        response.addHeader(HttpHeaders.SET_COOKIE, cookie.toString());
    }

    private void clearRefreshTokenCookie(HttpServletResponse response) {
        ResponseCookie cookie = ResponseCookie.from(REFRESH_TOKEN_COOKIE, "").httpOnly(true)
            .secure(cookieProperties.isSecure()).sameSite("Lax").path(COOKIE_PATH).maxAge(0).build();
        response.addHeader(HttpHeaders.SET_COOKIE, cookie.toString());
    }
}
