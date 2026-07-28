package top.harrylei.bitlog.comment.controller;

import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;
import top.harrylei.bitlog.api.model.comment.req.CommentSaveParam;
import top.harrylei.bitlog.api.model.comment.vo.CommentVO;
import top.harrylei.bitlog.comment.service.CommentService;
import top.harrylei.bitlog.common.context.ReqInfoContext;
import top.harrylei.bitlog.common.model.BasePage;
import top.harrylei.bitlog.common.model.PageVO;
import top.harrylei.bitlog.common.model.Result;
import top.harrylei.bitlog.common.security.RequiresLogin;

/**
 * 评论接口
 *
 * @author Harry
 * @since 2026-07-28
 */
@Tag(name = "评论接口")
@RestController
@RequestMapping("/api/v1/article/{articleId}/comments")
@RequiredArgsConstructor
public class CommentController {

    private final CommentService commentService;

    @Operation(summary = "分页查询文章评论（公开，含楼中楼回复）")
    @GetMapping("/page")
    public Result<PageVO<CommentVO>> page(@PathVariable Long articleId, @Valid BasePage query) {
        return Result.success(commentService.pageComments(articleId, query));
    }

    @RequiresLogin
    @Operation(summary = "发表评论或回复")
    @PostMapping
    public Result<Long> save(@PathVariable Long articleId, @Valid @RequestBody CommentSaveParam req) {
        return Result.success(commentService.saveComment(ReqInfoContext.getContext().getUserId(), articleId, req));
    }

    @RequiresLogin
    @Operation(summary = "删除本人评论")
    @DeleteMapping("/{commentId}")
    public Result<Void> delete(@PathVariable Long articleId, @PathVariable Long commentId) {
        commentService.deleteComment(ReqInfoContext.getContext().getUserId(), articleId, commentId);
        return Result.success();
    }
}
