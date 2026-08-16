package top.harrylei.bitlog.comment.controller;

import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PatchMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;
import top.harrylei.bitlog.api.model.comment.query.CommentAdminPageParam;
import top.harrylei.bitlog.api.model.comment.req.CommentBatchDeleteParam;
import top.harrylei.bitlog.api.model.comment.req.CommentStatusUpdateParam;
import top.harrylei.bitlog.api.model.comment.vo.CommentAdminVO;
import top.harrylei.bitlog.api.model.comment.vo.CommentStatsVO;
import top.harrylei.bitlog.comment.service.CommentService;
import top.harrylei.bitlog.common.model.PageVO;
import top.harrylei.bitlog.common.model.Result;
import top.harrylei.bitlog.common.security.RequiresAdmin;

/**
 * 评论管理接口
 *
 * @author Harry
 * @since 2026-07-28
 */
@Tag(name = "评论管理接口")
@RequiresAdmin
@RestController
@RequestMapping("/api/v1/admin/comments")
@RequiredArgsConstructor
public class AdminCommentController {

    private final CommentService commentService;

    @Operation(summary = "分页查询全站评论")
    @GetMapping("/page")
    public Result<PageVO<CommentAdminVO>> page(@Valid CommentAdminPageParam query) {
        return Result.success(commentService.pageForAdmin(query));
    }

    @Operation(summary = "统计全站评论各状态数量")
    @GetMapping("/stats")
    public Result<CommentStatsVO> stats(@Valid CommentAdminPageParam query) {
        return Result.success(commentService.getCommentStats(query));
    }

    @Operation(summary = "更新评论状态")
    @PatchMapping("/{commentId}/status")
    public Result<Void> updateStatus(@PathVariable Long commentId, @Valid @RequestBody CommentStatusUpdateParam req) {
        commentService.updateStatus(commentId, req.getStatus());
        return Result.success();
    }

    @Operation(summary = "批量删除评论")
    @DeleteMapping("/batch")
    public Result<Void> batchDelete(@Valid @RequestBody CommentBatchDeleteParam req) {
        commentService.batchDelete(req.getIds());
        return Result.success();
    }
}
