package top.harrylei.bitlog.link.controller;

import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;
import top.harrylei.bitlog.api.model.link.query.FriendLinkPageParam;
import top.harrylei.bitlog.api.model.link.req.FriendLinkAuditParam;
import top.harrylei.bitlog.api.model.link.req.FriendLinkSaveParam;
import top.harrylei.bitlog.api.model.link.vo.FriendLinkAdminVO;
import top.harrylei.bitlog.api.model.link.vo.FriendLinkStatsVO;
import top.harrylei.bitlog.common.model.PageVO;
import top.harrylei.bitlog.common.model.Result;
import top.harrylei.bitlog.common.security.RequiresAdmin;
import top.harrylei.bitlog.link.service.FriendLinkService;

/**
 * 友链管理接口
 * <p>
 * 独立挂在 /api/v1/admin 下，而非与 {@link FriendLinkController} 共用前缀， 这样公开白名单无论怎么写都不可能命中这里的写操作。
 * </p>
 *
 * @author Harry
 * @since 2026-08-15
 */
@Tag(name = "友链管理接口")
@RequiresAdmin
@RestController
@RequestMapping("/api/v1/admin/links")
@RequiredArgsConstructor
public class AdminFriendLinkController {

    private final FriendLinkService friendLinkService;

    @Operation(summary = "分页查询友链")
    @GetMapping("/page")
    public Result<PageVO<FriendLinkAdminVO>> page(@Valid FriendLinkPageParam query) {
        return Result.success(friendLinkService.pageForAdmin(query));
    }

    @Operation(summary = "统计友链各状态数量")
    @GetMapping("/stats")
    public Result<FriendLinkStatsVO> stats(@Valid FriendLinkPageParam query) {
        return Result.success(friendLinkService.getFriendLinkStats(query));
    }

    @Operation(summary = "录入友链，直接进入展示状态")
    @PostMapping
    public Result<Long> save(@Valid @RequestBody FriendLinkSaveParam req) {
        return Result.success(friendLinkService.saveByAdmin(req));
    }

    @Operation(summary = "修改友链内容")
    @PutMapping("/{id}")
    public Result<Void> update(@PathVariable Long id, @Valid @RequestBody FriendLinkSaveParam req) {
        friendLinkService.updateByAdmin(id, req);
        return Result.success();
    }

    @Operation(summary = "审核友链")
    @PutMapping("/{id}/audit")
    public Result<Void> audit(@PathVariable Long id, @Valid @RequestBody FriendLinkAuditParam req) {
        friendLinkService.audit(id, req);
        return Result.success();
    }

    @Operation(summary = "删除友链")
    @DeleteMapping("/{id}")
    public Result<Void> delete(@PathVariable Long id) {
        friendLinkService.deleteByAdmin(id);
        return Result.success();
    }
}
