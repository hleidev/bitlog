package top.harrylei.community.user.controller;

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
import top.harrylei.community.api.model.user.req.PasswordUpdateRequest;
import top.harrylei.community.api.model.user.req.UserUpdateRequest;
import top.harrylei.community.api.model.user.vo.UserDetailVO;
import top.harrylei.community.common.context.ReqInfoContext;
import top.harrylei.community.common.enums.ResultCode;
import top.harrylei.community.common.model.Result;
import top.harrylei.community.user.service.UserService;

/**
 * 用户接口
 *
 * @author harry
 * @since 0.0.1
 */
@Tag(name = "用户接口")
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
        Long userId = getCurrentUserId();
        return Result.success(userService.getUserDetail(userId));
    }

    /**
     * 更新当前用户基本信息
     */
    @Operation(summary = "更新用户基本信息")
    @PutMapping("/info")
    public Result<Void> updateInfo(@Valid @RequestBody UserUpdateRequest req) {
        Long userId = getCurrentUserId();
        userService.updateUserInfo(userId, req);
        return Result.success();
    }

    /**
     * 修改密码
     */
    @Operation(summary = "修改密码")
    @PutMapping("/password")
    public Result<Void> updatePassword(@Valid @RequestBody PasswordUpdateRequest req) {
        Long userId = getCurrentUserId();
        userService.updatePassword(userId, req);
        return Result.success();
    }

    /**
     * 更新头像
     */
    @Operation(summary = "更新头像")
    @PutMapping("/avatar")
    public Result<Void> updateAvatar(@RequestParam @NotBlank(message = "头像地址不能为空") @Size(max = 500, message = "头像地址过长") String avatar) {
        Long userId = getCurrentUserId();
        userService.updateAvatar(userId, avatar);
        return Result.success();
    }

    /**
     * 获取当前登录用户 ID，未登录则抛异常
     */
    private Long getCurrentUserId() {
        ReqInfoContext.ReqInfo context = ReqInfoContext.getContext();
        if (context.getUserId() == null) {
            ResultCode.TOKEN_INVALID.throwException();
        }
        return context.getUserId();
    }
}
