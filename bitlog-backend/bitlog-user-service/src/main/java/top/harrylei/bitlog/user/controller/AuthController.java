package top.harrylei.bitlog.user.controller;

import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;
import top.harrylei.bitlog.api.enums.user.UserRoleEnum;
import top.harrylei.bitlog.api.model.auth.LoginRequest;
import top.harrylei.bitlog.api.model.auth.UserCreateRequest;
import top.harrylei.bitlog.common.context.ReqInfoContext;
import top.harrylei.bitlog.common.enums.ResultCode;
import top.harrylei.bitlog.common.model.Result;
import top.harrylei.bitlog.user.service.AuthService;

/**
 * 认证接口
 *
 * @author harry
 * @since 0.0.1
 */
@RestController
@RequestMapping("/api/v1/auth")
@RequiredArgsConstructor
public class AuthController {

    private final AuthService authService;

    /**
     * 用户注册（普通用户自行注册）
     */
    @PostMapping("/register")
    public Result<Void> register(@Valid @RequestBody LoginRequest request) {
        authService.register(request.getUsername(), request.getPassword(), UserRoleEnum.NORMAL);
        return Result.success();
    }

    /**
     * 登录
     */
    @PostMapping("/login")
    public Result<String> login(@Valid @RequestBody LoginRequest request) {
        String token = authService.login(
                request.getUsername(),
                request.getPassword(),
                Boolean.TRUE.equals(request.getKeepLogin())
        );
        return Result.success(token);
    }

    /**
     * 退出登录
     */
    @PostMapping("/logout")
    public Result<Void> logout() {
        Long userId = ReqInfoContext.getContext().getUserId();
        if (userId == null) {
            ResultCode.TOKEN_INVALID.throwException();
        }
        authService.logout(userId);
        return Result.success();
    }

    /**
     * 管理员新建账号
     */
    @PostMapping("/admin/create")
    public Result<Void> createUser(@Valid @RequestBody UserCreateRequest request) {
        if (!ReqInfoContext.getContext().isAdmin()) {
            ResultCode.FORBIDDEN.throwException();
        }
        authService.register(request.getUsername(), request.getPassword(), request.getRole());
        return Result.success();
    }
}
