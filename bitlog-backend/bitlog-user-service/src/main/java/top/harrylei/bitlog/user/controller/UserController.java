package top.harrylei.bitlog.user.controller;

import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;
import top.harrylei.bitlog.api.model.user.req.PasswordUpdateRequest;
import top.harrylei.bitlog.api.model.user.req.UserUpdateRequest;
import top.harrylei.bitlog.api.model.user.vo.UserDetailVO;
import top.harrylei.bitlog.common.context.ReqInfoContext;
import top.harrylei.bitlog.common.model.Result;
import top.harrylei.bitlog.common.security.RequiresLogin;
import top.harrylei.bitlog.user.service.UserService;

/**
 * 用户接口
 *
 * @author harry
 * @since 0.0.1
 */
@Tag(name = "用户接口")
@RequiresLogin
@RestController
@RequestMapping("/api/v1/user")
@RequiredArgsConstructor
public class UserController {

    private final UserService userService;

    /**
     * 获取当前用户详情
     */
    @Operation(summary = "获取当前用户详情")
    @GetMapping("/profile")
    public Result<UserDetailVO> getProfile() {
        return Result.success(userService.getUserDetail(ReqInfoContext.getContext().getUserId()));
    }

    /**
     * 更新当前用户基本信息
     */
    @Operation(summary = "更新用户基本信息")
    @PutMapping("/info")
    public Result<Void> updateInfo(@Valid @RequestBody UserUpdateRequest req) {
        userService.updateUserInfo(ReqInfoContext.getContext().getUserId(), req);
        return Result.success();
    }

    /**
     * 修改密码
     */
    @Operation(summary = "修改密码")
    @PutMapping("/password")
    public Result<Void> updatePassword(@Valid @RequestBody PasswordUpdateRequest req) {
        userService.updatePassword(ReqInfoContext.getContext().getUserId(), req);
        return Result.success();
    }

    /**
     * 更新头像
     */
    @Operation(summary = "更新头像")
    @PutMapping("/avatar")
    public Result<Void> updateAvatar(@RequestParam @NotBlank(message = "头像地址不能为空") @Size(max = 500, message = "头像地址过长") String avatar) {
        userService.updateAvatar(ReqInfoContext.getContext().getUserId(), avatar);
        return Result.success();
    }
}
