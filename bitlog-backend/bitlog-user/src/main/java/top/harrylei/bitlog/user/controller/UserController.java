package top.harrylei.bitlog.user.controller;

import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;
import lombok.RequiredArgsConstructor;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.CookieValue;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;
import top.harrylei.bitlog.api.model.user.req.EmailCodeParam;
import top.harrylei.bitlog.api.model.user.req.EmailUpdateParam;
import top.harrylei.bitlog.api.model.user.req.PasswordInitParam;
import top.harrylei.bitlog.api.model.user.req.PasswordUpdateParam;
import top.harrylei.bitlog.api.model.user.req.UserUpdateParam;
import top.harrylei.bitlog.api.model.user.vo.UserDetailVO;
import top.harrylei.bitlog.api.model.user.vo.UserIdentityVO;
import top.harrylei.bitlog.common.context.ReqInfoContext;
import top.harrylei.bitlog.common.model.Result;
import top.harrylei.bitlog.common.security.RequiresLogin;
import top.harrylei.bitlog.user.component.RefreshTokenCookie;
import top.harrylei.bitlog.user.service.UserService;

import java.util.List;

/**
 * 用户接口
 *
 * @author Harry
 * @since 2026-03-28
 */
@Tag(name = "用户接口")
@Validated
@RequiresLogin
@RestController
@RequestMapping("/api/v1/user")
@RequiredArgsConstructor
public class UserController {

    private final UserService userService;

    @Operation(summary = "获取当前用户详情")
    @GetMapping("/profile")
    public Result<UserDetailVO> getProfile() {
        return Result.success(userService.getUserDetail(ReqInfoContext.getContext().getUserId()));
    }

    @Operation(summary = "更新用户基本信息")
    @PutMapping("/info")
    public Result<Void> updateInfo(@Valid @RequestBody UserUpdateParam req) {
        userService.updateUserInfo(ReqInfoContext.getContext().getUserId(), req);
        return Result.success();
    }

    @Operation(summary = "修改密码")
    @PutMapping("/password")
    public Result<Void> updatePassword(@Valid @RequestBody PasswordUpdateParam req,
        @CookieValue(name = RefreshTokenCookie.COOKIE_NAME, required = false) String refreshToken) {
        userService.updatePassword(ReqInfoContext.getContext().getUserId(), req, refreshToken);
        return Result.success();
    }

    @Operation(summary = "首次设置密码")
    @PostMapping("/password")
    public Result<Void> initPassword(@Valid @RequestBody PasswordInitParam req,
        @CookieValue(name = RefreshTokenCookie.COOKIE_NAME, required = false) String refreshToken) {
        userService.initPassword(ReqInfoContext.getContext().getUserId(), req, refreshToken);
        return Result.success();
    }

    @Operation(summary = "查询已绑定的第三方身份")
    @GetMapping("/identities")
    public Result<List<UserIdentityVO>> listIdentities() {
        return Result.success(userService.listIdentities(ReqInfoContext.getContext().getUserId()));
    }

    @Operation(summary = "发起第三方账号绑定，返回授权入口所需的意图令牌")
    @PostMapping("/identities/bind-intent")
    public Result<String> createBindIntent() {
        return Result.success(userService.createBindIntent(ReqInfoContext.getContext().getUserId()));
    }

    @Operation(summary = "解绑第三方身份")
    @DeleteMapping("/identities/{provider}")
    public Result<Void> unbindIdentity(@PathVariable @NotBlank(message = "平台标识不能为空") String provider) {
        userService.unbindIdentity(ReqInfoContext.getContext().getUserId(), provider);
        return Result.success();
    }

    @Operation(summary = "发送修改邮箱验证码")
    @PostMapping("/email/code")
    public Result<Void> sendEmailChangeCode(@Valid @RequestBody EmailCodeParam req) {
        userService.sendEmailChangeCode(ReqInfoContext.getContext().getUserId(), req);
        return Result.success();
    }

    @Operation(summary = "修改邮箱")
    @PutMapping("/email")
    public Result<Void> updateEmail(@Valid @RequestBody EmailUpdateParam req) {
        userService.updateEmail(ReqInfoContext.getContext().getUserId(), req);
        return Result.success();
    }

    @Operation(summary = "更新头像")
    @PutMapping("/avatar")
    public Result<Void>
        updateAvatar(@RequestParam @NotBlank(message = "头像地址不能为空") @Size(max = 256, message = "头像地址过长") String avatar) {
        userService.updateAvatar(ReqInfoContext.getContext().getUserId(), avatar);
        return Result.success();
    }

    @Operation(summary = "注销账号")
    @DeleteMapping
    public Result<Void> deactivateAccount() {
        userService.deactivateUserBatch(List.of(ReqInfoContext.getContext().getUserId()));
        return Result.success();
    }
}
