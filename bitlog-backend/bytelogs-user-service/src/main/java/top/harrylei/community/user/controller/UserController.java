package top.harrylei.community.user.controller;

import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.ModelAttribute;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;
import top.harrylei.community.api.model.user.query.UserFollowPageQuery;
import top.harrylei.community.api.model.user.req.PasswordUpdateRequest;
import top.harrylei.community.api.model.user.req.UserUpdateRequest;
import top.harrylei.community.api.model.user.vo.UserDetailVO;
import top.harrylei.community.api.model.user.vo.UserFollowVO;
import top.harrylei.community.common.context.ReqInfoContext;
import top.harrylei.community.common.enums.ResultCode;
import top.harrylei.community.common.model.PageVO;
import top.harrylei.community.common.model.Result;
import top.harrylei.community.user.service.UserFollowService;
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
    private final UserFollowService userFollowService;

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
     * 关注用户
     */
    @Operation(summary = "关注用户")
    @PostMapping("/{followUserId}/follow")
    public Result<Void> follow(@PathVariable Long followUserId) {
        Long userId = getCurrentUserId();
        userFollowService.follow(userId, followUserId);
        return Result.success();
    }

    /**
     * 取消关注
     */
    @Operation(summary = "取消关注")
    @DeleteMapping("/{followUserId}/follow")
    public Result<Void> unfollow(@PathVariable Long followUserId) {
        Long userId = getCurrentUserId();
        userFollowService.unfollow(userId, followUserId);
        return Result.success();
    }

    /**
     * 查询当前用户的关注列表
     */
    @Operation(summary = "关注列表")
    @GetMapping("/following")
    public Result<PageVO<UserFollowVO>> following(@ModelAttribute @Valid UserFollowPageQuery query) {
        Long userId = getCurrentUserId();
        query.setUserId(userId);
        return Result.success(userFollowService.pageFollowing(query));
    }

    /**
     * 查询当前用户的粉丝列表
     */
    @Operation(summary = "粉丝列表")
    @GetMapping("/followers")
    public Result<PageVO<UserFollowVO>> followers(@ModelAttribute @Valid UserFollowPageQuery query) {
        Long userId = getCurrentUserId();
        query.setUserId(userId);
        return Result.success(userFollowService.pageFollowers(query));
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
