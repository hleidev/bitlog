package top.harrylei.bitlog.article.controller;

import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;
import top.harrylei.bitlog.api.model.article.dto.ArticleDTO;
import top.harrylei.bitlog.article.service.ArticleService;
import top.harrylei.bitlog.common.model.Result;

import java.util.List;

/**
 * 文章内部接口
 *
 * @author harry
 * @since 0.0.1
 */
@Tag(name = "文章内部接口")
@RestController
@RequestMapping("/api/v1/internal/article")
@RequiredArgsConstructor
public class InternalArticleController {

    private final ArticleService articleService;

    @Operation(summary = "根据文章 ID 获取文章基础信息")
    @GetMapping("/{articleId}")
    public Result<ArticleDTO> getArticleById(@PathVariable Long articleId) {
        return Result.success(articleService.getArticleDTO(articleId));
    }

    @Operation(summary = "批量查询文章基础信息")
    @PostMapping("/batch")
    public Result<List<ArticleDTO>> getArticleByIds(@RequestBody List<Long> articleIds) {
        return Result.success(articleService.getArticleDTOBatch(articleIds));
    }
}
