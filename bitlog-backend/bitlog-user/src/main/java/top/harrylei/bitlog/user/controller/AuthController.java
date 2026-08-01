package top.harrylei.bitlog.user.controller;

import jakarta.servlet.http.HttpServletResponse;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.util.StringUtils;
import org.springframework.web.bind.annotation.*;
import top.harrylei.bitlog.api.model.auth.LoginParam;
import top.harrylei.bitlog.api.model.auth.EmailParam;
import top.harrylei.bitlog.api.model.auth.LoginVO;
import top.harrylei.bitlog.api.model.auth.PasswordResetParam;
import top.harrylei.bitlog.api.model.auth.RegisterParam;
import top.harrylei.bitlog.common.enums.ResultCode;
import top.harrylei.bitlog.common.model.Result;
import top.harrylei.bitlog.user.component.RefreshTokenCookie;
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

    private static final String REFRESH_TOKEN_COOKIE = RefreshTokenCookie.COOKIE_NAME;

    private final AuthService authService;
    private final RefreshTokenCookie refreshTokenCookie;

    @PostMapping("/register/code")
    public Result<Void> sendRegisterCode(@Valid @RequestBody EmailParam request) {
        authService.sendRegisterCode(request.getEmail());
        return Result.success();
    }

    @PostMapping("/register")
    public Result<Void> register(@Valid @RequestBody RegisterParam request) {
        authService.register(request);
        return Result.success();
    }

    @PostMapping("/login")
    public Result<LoginVO> login(@Valid @RequestBody LoginParam request, HttpServletResponse response) {
        LoginResult result = authService.login(request);
        refreshTokenCookie.write(response, result.refreshToken());
        return Result.success(new LoginVO(result.accessToken()));
    }

    @PostMapping("/refresh")
    public Result<LoginVO> refresh(@CookieValue(name = REFRESH_TOKEN_COOKIE, required = false) String refreshToken,
        HttpServletResponse response) {
        if (!StringUtils.hasText(refreshToken)) {
            return Result.fail(ResultCode.REFRESH_TOKEN_INVALID);
        }
        LoginResult result = authService.refresh(refreshToken);
        refreshTokenCookie.write(response, result.refreshToken());
        return Result.success(new LoginVO(result.accessToken()));
    }

    @PostMapping("/logout")
    public Result<Void> logout(@CookieValue(name = REFRESH_TOKEN_COOKIE, required = false) String refreshToken,
        HttpServletResponse response) {
        authService.logout(refreshToken);
        refreshTokenCookie.clear(response);
        return Result.success();
    }

    @PostMapping("/password/reset/code")
    public Result<Void> sendResetPasswordCode(@Valid @RequestBody EmailParam request) {
        authService.sendResetPasswordCode(request.getEmail());
        return Result.success();
    }

    @PostMapping("/password/reset")
    public Result<Void> resetPassword(@Valid @RequestBody PasswordResetParam request) {
        authService.resetPassword(request);
        return Result.success();
    }

}
