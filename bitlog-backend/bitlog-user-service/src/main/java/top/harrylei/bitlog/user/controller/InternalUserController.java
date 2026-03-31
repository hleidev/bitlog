package top.harrylei.bitlog.user.controller;

import io.swagger.v3.oas.annotations.Hidden;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.*;
import top.harrylei.bitlog.api.model.user.vo.UserVO;
import top.harrylei.bitlog.common.model.Result;
import top.harrylei.bitlog.user.service.UserService;

import java.util.List;

/**
 * 用户服务接口
 *
 * @author harry
 * @since 0.0.1
 */
@Hidden
@RestController
@RequestMapping("/api/v1/internal/user")
@RequiredArgsConstructor
public class InternalUserController {

    private final UserService userService;

    /**
     * 根据用户 ID 查询用户基础信息
     *
     * @param userId 用户 ID
     * @return 用户基础信息
     */
    @GetMapping("/{userId}")
    public Result<UserVO> getUserInfo(@PathVariable Long userId) {
        return Result.success(userService.getUserById(userId));
    }

    /**
     * 批量查询用户基础信息
     *
     * @param userIds 用户 ID 列表
     * @return 用户基础信息列表
     */
    @GetMapping("/batch")
    public Result<List<UserVO>> getUserInfoBatch(@RequestParam List<Long> userIds) {
        return Result.success(userService.getUserBatchByIds(userIds));
    }
}
