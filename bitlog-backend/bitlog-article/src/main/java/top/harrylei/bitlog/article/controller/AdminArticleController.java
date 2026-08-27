package top.harrylei.bitlog.article.controller;

import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PatchMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;
import top.harrylei.bitlog.article.model.query.MyArticlePageParam;
import top.harrylei.bitlog.article.model.req.ArticleBatchDeleteParam;
import top.harrylei.bitlog.article.model.req.ArticleBatchStatusUpdateParam;
import top.harrylei.bitlog.article.model.req.ArticleMetaUpdateParam;
import top.harrylei.bitlog.article.model.req.ArticlePublishParam;
import top.harrylei.bitlog.article.model.req.ArticleSaveParam;
import top.harrylei.bitlog.article.model.req.ArticleVersionBatchDeleteParam;
import top.harrylei.bitlog.article.model.vo.ArticleCountVO;
import top.harrylei.bitlog.article.model.vo.ArticleDetailVO;
import top.harrylei.bitlog.article.model.vo.ArticleVersionDetailVO;
import top.harrylei.bitlog.article.model.vo.ArticleVersionVO;
import top.harrylei.bitlog.article.model.vo.ArticleVO;
import top.harrylei.bitlog.article.service.ArticleService;
import top.harrylei.bitlog.common.context.ReqInfoContext;
import top.harrylei.bitlog.common.model.PageVO;
import top.harrylei.bitlog.common.model.Result;
import top.harrylei.bitlog.common.security.RequiresAdmin;

import java.util.List;

/**
 * 管理员文章接口，与 {@link ArticleController} 共用前缀
 *
 * @author Harry
 * @since 2026-08-09
 */
@Tag(name = "管理员文章接口")
@RequiresAdmin
@RestController
@RequestMapping("/api/v1/article")
@RequiredArgsConstructor
public class AdminArticleController {

    private final ArticleService articleService;

    @Operation(summary = "新建文章草稿")
    @PostMapping
    public Result<Long> save(@Valid @RequestBody ArticleSaveParam req) {
        return Result.success(articleService.saveArticle(ReqInfoContext.getContext().getUserId(), req));
    }

    @Operation(summary = "更新文章草稿（生成新版本）")
    @PutMapping("/{id}")
    public Result<Void> update(@PathVariable Long id, @Valid @RequestBody ArticleSaveParam req) {
        articleService.updateArticle(ReqInfoContext.getContext().getUserId(), id, req);
        return Result.success();
    }

    @Operation(summary = "发布文章（设置封面、摘要、分类、标签并发布）")
    @PostMapping("/{id}/publish")
    public Result<Void> publish(@PathVariable Long id, @Valid @RequestBody ArticlePublishParam req) {
        articleService.publishArticle(ReqInfoContext.getContext().getUserId(), id, req);
        return Result.success();
    }

    @Operation(summary = "放弃未发布的草稿改动（草稿头回退到已发布版本）")
    @PostMapping("/{id}/draft/discard")
    public Result<Void> discardDraft(@PathVariable Long id) {
        articleService.discardDraftAbovePublish(ReqInfoContext.getContext().getUserId(), id);
        return Result.success();
    }

    @Operation(summary = "快速更新文章元数据（摘要、分类、标签），不影响内容与版本")
    @PatchMapping("/{id}/meta")
    public Result<Void> updateMeta(@PathVariable Long id, @Valid @RequestBody ArticleMetaUpdateParam req) {
        articleService.updateArticleMeta(ReqInfoContext.getContext().getUserId(), id, req);
        return Result.success();
    }

    @Operation(summary = "批量切换文章状态（传单个 ID 即为单篇操作）")
    @PatchMapping("/batch/status")
    public Result<Void> batchUpdateStatus(@Valid @RequestBody ArticleBatchStatusUpdateParam req) {
        articleService.batchUpdateStatus(ReqInfoContext.getContext().getUserId(), req.getIds(), req.getStatus());
        return Result.success();
    }

    @Operation(summary = "批量删除文章")
    @DeleteMapping("/batch")
    public Result<Void> batchDelete(@Valid @RequestBody ArticleBatchDeleteParam req) {
        articleService.batchDelete(ReqInfoContext.getContext().getUserId(), req.getIds());
        return Result.success();
    }

    @Operation(summary = "获取文章草稿（作者编辑视角）")
    @GetMapping("/{id}/draft")
    public Result<ArticleDetailVO> draft(@PathVariable Long id) {
        return Result.success(articleService.getDraftDetail(ReqInfoContext.getContext().getUserId(), id));
    }

    @Operation(summary = "获取文章版本历史")
    @GetMapping("/{id}/versions")
    public Result<List<ArticleVersionVO>> versions(@PathVariable Long id) {
        return Result.success(articleService.listVersions(ReqInfoContext.getContext().getUserId(), id));
    }

    @Operation(summary = "获取指定版本详情（含正文，用于版本对比）")
    @GetMapping("/{id}/versions/{versionId}")
    public Result<ArticleVersionDetailVO> versionDetail(@PathVariable Long id, @PathVariable Long versionId) {
        return Result.success(articleService.getVersionDetail(ReqInfoContext.getContext().getUserId(), id, versionId));
    }

    @Operation(summary = "批量删除文章版本（传单个 ID 即为单个操作）")
    @DeleteMapping("/{id}/versions/batch")
    public Result<Void> batchDeleteVersions(@PathVariable Long id,
        @Valid @RequestBody ArticleVersionBatchDeleteParam req) {
        articleService.deleteVersions(ReqInfoContext.getContext().getUserId(), id, req.getVersionIds());
        return Result.success();
    }

    @Operation(summary = "回滚到指定版本")
    @PostMapping("/{id}/versions/{versionId}/rollback")
    public Result<Void> rollback(@PathVariable Long id, @PathVariable Long versionId) {
        articleService.rollbackVersion(ReqInfoContext.getContext().getUserId(), id, versionId);
        return Result.success();
    }

    @Operation(summary = "分页查询我的文章列表（含草稿）")
    @GetMapping("/my")
    public Result<PageVO<ArticleVO>> myArticles(@Valid MyArticlePageParam query) {
        return Result.success(articleService.pageMyArticles(ReqInfoContext.getContext().getUserId(), query));
    }

    @Operation(summary = "统计我的文章各状态数量（关键词参与过滤，与列表口径一致）")
    @GetMapping("/my/stats")
    public Result<ArticleCountVO> myArticleStats(@Valid MyArticlePageParam query) {
        return Result.success(articleService.countMyArticles(ReqInfoContext.getContext().getUserId(), query));
    }
}
