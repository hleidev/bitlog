package top.harrylei.bitlog.user.controller;

import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.RequiredArgsConstructor;
import org.springdoc.core.annotations.ParameterObject;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;
import top.harrylei.bitlog.api.model.user.query.UserPageQuery;
import top.harrylei.bitlog.api.model.user.vo.UserListVO;
import top.harrylei.bitlog.common.model.PageVO;
import top.harrylei.bitlog.common.model.Result;
import top.harrylei.bitlog.common.security.RequiresAdmin;
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

    private final UserService userService;

    /**
     * 分页查询用户列表
     */
    @Operation(summary = "分页查询用户列表")
    @GetMapping("/users")
    public Result<PageVO<UserListVO>> pageUsers(@ParameterObject UserPageQuery query) {
        return Result.success(userService.pageQuery(query));
    }
}
