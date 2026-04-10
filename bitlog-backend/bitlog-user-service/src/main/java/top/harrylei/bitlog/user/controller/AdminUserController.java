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
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;
import top.harrylei.bitlog.api.model.user.query.UserPageQuery;
import top.harrylei.bitlog.api.model.user.req.AdminCreateUserRequest;
import top.harrylei.bitlog.api.model.user.req.UserIdsRequest;
import top.harrylei.bitlog.api.model.user.req.UserStatusUpdateRequest;
import top.harrylei.bitlog.api.model.user.vo.PasswordResetVO;
import top.harrylei.bitlog.api.model.user.vo.UserCreatedVO;
import top.harrylei.bitlog.api.model.user.vo.UserDetailVO;
import top.harrylei.bitlog.api.model.user.vo.UserListVO;
import top.harrylei.bitlog.api.model.user.vo.UserStatsVO;
import top.harrylei.bitlog.common.model.PageVO;
import top.harrylei.bitlog.common.model.Result;
import top.harrylei.bitlog.common.security.RequiresAdmin;
import top.harrylei.bitlog.user.service.AuthService;
import top.harrylei.bitlog.user.service.UserService;

/**
 * 管理员用户接口
 *
 * @author harry
 * @since 0.0.1
 */
@Tag(name = "管理员用户接口")
@RequiresAdmin
@RestController
@RequestMapping("/api/v1/admin")
@RequiredArgsConstructor
public class AdminUserController {

    private final AuthService authService;
    private final UserService userService;

    /**
     * 创建用户
     */
    @Operation(summary = "创建用户")
    @PostMapping("/users")
    public Result<UserCreatedVO> createUser(@Valid @RequestBody AdminCreateUserRequest req) {
        return Result.success(authService.adminCreateUser(req));
    }

    /**
     * 分页查询用户列表
     */
    @Operation(summary = "分页查询用户列表")
    @GetMapping("/users")
    public Result<PageVO<UserListVO>> pageUsers(@ParameterObject UserPageQuery query) {
        return Result.success(userService.pageQuery(query));
    }

    /**
     * 查询用户数量统计
     */
    @Operation(summary = "查询用户数量统计")
    @GetMapping("/users/stats")
    public Result<UserStatsVO> getUserStats() {
        return Result.success(userService.getUserStats());
    }

    /**
     * 查询用户完整信息
     */
    @Operation(summary = "查询用户完整信息")
    @GetMapping("/users/{userId}")
    public Result<UserDetailVO> getUserDetail(@PathVariable Long userId) {
        return Result.success(userService.getUserDetail(userId));
    }

    /**
     * 批量修改用户状态
     */
    @Operation(summary = "批量修改用户状态")
    @PatchMapping("/users/status")
    public Result<Void> updateUserStatus(@Valid @RequestBody UserStatusUpdateRequest req) {
        userService.updateUserStatusBatch(req.getUserIds(), req.getStatus());
        return Result.success();
    }

    /**
     * 批量软删除用户
     */
    @Operation(summary = "批量软删除用户")
    @DeleteMapping("/users")
    public Result<Void> deleteUsers(@Valid @RequestBody UserIdsRequest req) {
        userService.deleteUserBatch(req.getUserIds());
        return Result.success();
    }

    /**
     * 批量恢复已删除用户
     */
    @Operation(summary = "批量恢复已删除用户")
    @PatchMapping("/users/restore")
    public Result<Void> restoreUsers(@Valid @RequestBody UserIdsRequest req) {
        userService.restoreUserBatch(req.getUserIds());
        return Result.success();
    }

    /**
     * 批量物理删除用户（不可恢复）
     */
    @Operation(summary = "批量物理删除用户（不可恢复）")
    @DeleteMapping("/users/permanent")
    public Result<Void> removeUsers(@Valid @RequestBody UserIdsRequest req) {
        userService.removeUserBatch(req.getUserIds());
        return Result.success();
    }

    /**
     * 重置用户密码
     */
    @Operation(summary = "重置用户密码")
    @PostMapping("/users/{userId}/password/reset")
    public Result<PasswordResetVO> resetPassword(@PathVariable Long userId) {
        return Result.success(userService.resetPassword(userId));
    }
}
