package top.harrylei.bitlog.api.feign;

import org.springframework.cloud.openfeign.FeignClient;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import top.harrylei.bitlog.api.model.article.dto.ArticleDTO;
import top.harrylei.bitlog.common.model.Result;

import java.util.List;

/**
 * 文章服务远程调用接口
 *
 * @author harry
 * @since 0.0.1
 */
@FeignClient(name = "bitlog-article-service", path = "/api/v1/internal/article")
public interface ArticleClient {

    /**
     * 根据文章 ID 查询文章基础信息
     *
     * @param articleId 文章 ID
     * @return 文章信息
     */
    @GetMapping("/{articleId}")
    Result<ArticleDTO> getArticleById(@PathVariable Long articleId);

    /**
     * 批量查询文章基础信息
     *
     * @param articleIds 文章 ID 列表
     * @return 文章信息列表
     */
    @PostMapping("/batch")
    Result<List<ArticleDTO>> getArticleByIds(@RequestBody List<Long> articleIds);
}
