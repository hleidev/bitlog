package top.harrylei.bitlog.article.controller;

import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;
import top.harrylei.bitlog.api.model.article.vo.AiArticleMetadataVO;
import top.harrylei.bitlog.article.service.ArticleAiService;
import top.harrylei.bitlog.common.context.ReqInfoContext;
import top.harrylei.bitlog.common.model.Result;
import top.harrylei.bitlog.common.security.RequiresAdmin;

/**
 * 文章 AI 接口
 *
 * @author Harry
 * @since 2026-05-24
 */
@Tag(name = "文章 AI 接口")
@RestController
@RequestMapping("/api/v1/article")
@RequiredArgsConstructor
public class ArticleAiController {

    private final ArticleAiService articleAiService;

    @RequiresAdmin
    @Operation(summary = "AI 生成文章元数据推荐（摘要 + 分类 + 标签）")
    @PostMapping("/{id}/ai/metadata")
    public Result<AiArticleMetadataVO> generateMetadata(@PathVariable Long id) {
        return Result.success(articleAiService.generateMetadata(ReqInfoContext.getContext().getUserId(), id));
    }
}
