package top.harrylei.bitlog.article.service;

import top.harrylei.bitlog.article.model.vo.AiArticleMetadataVO;

/**
 * 文章 AI 辅助服务
 *
 * @author Harry
 * @since 2026-05-24
 */
public interface ArticleAiService {

    /**
     * 根据文章内容，AI 一次性生成摘要、分类推荐和标签推荐
     *
     * @param userId 当前用户 ID（鉴权用）
     * @param articleId 文章 ID
     * @return AI 推荐的文章元数据
     */
    AiArticleMetadataVO generateMetadata(Long userId, Long articleId);
}
