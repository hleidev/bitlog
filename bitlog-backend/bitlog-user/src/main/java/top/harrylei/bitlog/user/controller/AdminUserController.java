package top.harrylei.bitlog.user.controller;

import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springdoc.core.annotations.ParameterObject;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PatchMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;
import top.harrylei.bitlog.api.model.user.query.UserPageParam;
import top.harrylei.bitlog.api.model.user.req.UserIdsParam;
import top.harrylei.bitlog.api.model.user.req.UserStatusUpdateParam;
import top.harrylei.bitlog.api.model.user.vo.UserDetailVO;
import top.harrylei.bitlog.api.model.user.vo.UserListVO;
import top.harrylei.bitlog.api.model.user.vo.UserStatsVO;
import top.harrylei.bitlog.common.model.PageVO;
import top.harrylei.bitlog.common.model.Result;
import top.harrylei.bitlog.common.security.RequiresAdmin;
import top.harrylei.bitlog.user.service.UserService;

/**
 * 管理员用户接口
 *
 * @author Harry
 * @since 2026-04-09
 */
@Tag(name = "管理员用户接口")
@RequiresAdmin
@RestController
@RequestMapping("/api/v1/admin")
@RequiredArgsConstructor
public class AdminUserController {

    private final UserService userService;

    @Operation(summary = "分页查询用户列表")
    @GetMapping("/users")
    public Result<PageVO<UserListVO>> pageUsers(@Valid @ParameterObject UserPageParam query) {
        return Result.success(userService.pageQuery(query));
    }

    @Operation(summary = "查询用户数量统计")
    @GetMapping("/users/stats")
    public Result<UserStatsVO> getUserStats(@Valid @ParameterObject UserPageParam query) {
        return Result.success(userService.getUserStats(query));
    }

    @Operation(summary = "查询用户完整信息")
    @GetMapping("/users/{userId}")
    public Result<UserDetailVO> getUserDetail(@PathVariable Long userId) {
        return Result.success(userService.getUserDetailIncludingDeactivated(userId));
    }

    @Operation(summary = "批量修改用户状态")
    @PatchMapping("/users/status")
    public Result<Void> updateUserStatus(@Valid @RequestBody UserStatusUpdateParam req) {
        userService.updateUserStatusBatch(req.getUserIds(), req.getStatus());
        return Result.success();
    }

    @Operation(summary = "批量强制注销用户")
    @DeleteMapping("/users")
    public Result<Void> deactivateUsers(@Valid @RequestBody UserIdsParam req) {
        userService.deactivateUserBatch(req.getUserIds());
        return Result.success();
    }
}
