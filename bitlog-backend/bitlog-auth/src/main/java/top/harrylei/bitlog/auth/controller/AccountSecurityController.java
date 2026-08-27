package top.harrylei.bitlog.auth.controller;

import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import jakarta.validation.constraints.NotBlank;
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
import org.springframework.web.bind.annotation.RestController;
import top.harrylei.bitlog.auth.model.EmailCodeParam;
import top.harrylei.bitlog.auth.model.EmailUpdateParam;
import top.harrylei.bitlog.auth.model.PasswordInitParam;
import top.harrylei.bitlog.auth.model.PasswordUpdateParam;
import top.harrylei.bitlog.auth.model.UserIdentityVO;
import top.harrylei.bitlog.common.context.ReqInfoContext;
import top.harrylei.bitlog.common.model.Result;
import top.harrylei.bitlog.common.security.RequiresLogin;
import top.harrylei.bitlog.auth.support.RefreshTokenCookie;
import top.harrylei.bitlog.auth.service.AccountCredentialService;

import java.util.List;

/**
 * 账号安全接口
 * <p>
 * 路径仍挂在 /api/v1/user 之下：白名单按「是否需要登录」划分，这些端点都要登录态， 与免登录的 /api/v1/auth 分属两类，不因实现归属认证侧而改变对外路径。
 * </p>
 *
 * @author Harry
 * @since 2026-08-09
 */
@Tag(name = "账号安全接口")
@Validated
@RequiresLogin
@RestController
@RequestMapping("/api/v1/user")
@RequiredArgsConstructor
public class AccountSecurityController {

    private final AccountCredentialService accountCredentialService;

    @Operation(summary = "修改密码")
    @PutMapping("/password")
    public Result<Void> updatePassword(@Valid @RequestBody PasswordUpdateParam req,
        @CookieValue(name = RefreshTokenCookie.COOKIE_NAME, required = false) String refreshToken) {
        accountCredentialService.updatePassword(ReqInfoContext.getContext().getUserId(), req, refreshToken);
        return Result.success();
    }

    @Operation(summary = "首次设置密码")
    @PostMapping("/password")
    public Result<Void> initPassword(@Valid @RequestBody PasswordInitParam req,
        @CookieValue(name = RefreshTokenCookie.COOKIE_NAME, required = false) String refreshToken) {
        accountCredentialService.initPassword(ReqInfoContext.getContext().getUserId(), req, refreshToken);
        return Result.success();
    }

    @Operation(summary = "查询已绑定的第三方身份")
    @GetMapping("/identities")
    public Result<List<UserIdentityVO>> listIdentities() {
        return Result.success(accountCredentialService.listIdentities(ReqInfoContext.getContext().getUserId()));
    }

    @Operation(summary = "发起第三方账号绑定，返回授权入口所需的意图令牌")
    @PostMapping("/identities/bind-intent")
    public Result<String> createBindIntent() {
        return Result.success(accountCredentialService.createBindIntent(ReqInfoContext.getContext().getUserId()));
    }

    @Operation(summary = "解绑第三方身份")
    @DeleteMapping("/identities/{provider}")
    public Result<Void> unbindIdentity(@PathVariable @NotBlank(message = "平台标识不能为空") String provider) {
        accountCredentialService.unbindIdentity(ReqInfoContext.getContext().getUserId(), provider);
        return Result.success();
    }

    @Operation(summary = "发送修改邮箱验证码")
    @PostMapping("/email/code")
    public Result<Void> sendEmailChangeCode(@Valid @RequestBody EmailCodeParam req) {
        accountCredentialService.sendEmailChangeCode(ReqInfoContext.getContext().getUserId(), req);
        return Result.success();
    }

    @Operation(summary = "修改邮箱")
    @PutMapping("/email")
    public Result<Void> updateEmail(@Valid @RequestBody EmailUpdateParam req) {
        accountCredentialService.updateEmail(ReqInfoContext.getContext().getUserId(), req);
        return Result.success();
    }
}
