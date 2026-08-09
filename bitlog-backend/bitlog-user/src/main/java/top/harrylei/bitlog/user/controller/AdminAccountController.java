package top.harrylei.bitlog.user.controller;

import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;
import top.harrylei.bitlog.api.model.user.req.AdminCreateUserParam;
import top.harrylei.bitlog.api.model.user.vo.PasswordResetVO;
import top.harrylei.bitlog.api.model.user.vo.UserCreatedVO;
import top.harrylei.bitlog.common.model.Result;
import top.harrylei.bitlog.common.security.RequiresAdmin;
import top.harrylei.bitlog.user.service.AccountCredentialService;
import top.harrylei.bitlog.user.service.AuthService;

/**
 * 管理员账号凭据接口
 * <p>
 * 与 {@link AdminUserController} 同挂 /api/v1/admin，按是否产出凭据分开： 建号返回初始密码、重置密码改写凭据，均属认证侧；用户的查询、启停与注销归用户侧。
 * </p>
 *
 * @author Harry
 * @since 2026-08-09
 */
@Tag(name = "管理员账号凭据接口")
@RequiresAdmin
@RestController
@RequestMapping("/api/v1/admin")
@RequiredArgsConstructor
public class AdminAccountController {

    private final AuthService authService;
    private final AccountCredentialService accountCredentialService;

    @Operation(summary = "创建用户")
    @PostMapping("/users")
    public Result<UserCreatedVO> createUser(@Valid @RequestBody AdminCreateUserParam req) {
        return Result.success(authService.adminCreateUser(req));
    }

    @Operation(summary = "重置用户密码")
    @PostMapping("/users/{userId}/password/reset")
    public Result<PasswordResetVO> resetPassword(@PathVariable Long userId) {
        return Result.success(accountCredentialService.resetPassword(userId));
    }
}
