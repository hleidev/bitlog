package top.harrylei.bitlog.article.controller;

import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.*;
import top.harrylei.bitlog.api.model.article.query.ArticlePageParam;
import top.harrylei.bitlog.api.model.article.vo.ArticlePublicDetailVO;
import top.harrylei.bitlog.api.model.article.vo.ArticlePublicVO;
import top.harrylei.bitlog.article.service.ArticleService;
import top.harrylei.bitlog.common.model.PageVO;
import top.harrylei.bitlog.common.model.Result;

/**
 * 文章接口，仅公开读取，写操作见 {@link AdminArticleController}
 *
 * @author Harry
 * @since 2026-04-09
 */
@Tag(name = "文章接口")
@RestController
@RequestMapping("/api/v1/article")
@RequiredArgsConstructor
public class ArticleController {

    private final ArticleService articleService;

    @Operation(summary = "获取文章详情（已发布，读者视角）")
    @GetMapping("/{id}")
    public Result<ArticlePublicDetailVO> detail(@PathVariable Long id) {
        return Result.success(articleService.getPublishedDetail(id));
    }

    @Operation(summary = "分页查询已发布文章列表（公开）")
    @GetMapping("/page")
    public Result<PageVO<ArticlePublicVO>> pagePublished(@Valid ArticlePageParam query) {
        return Result.success(articleService.pagePublished(query));
    }
}
