package top.harrylei.bitlog.article.service;

/**
 * 文章 AI 辅助服务
 *
 * @author Harry
 * @since 2026-05-24
 */
public interface ArticleAiService {

    /**
     * 根据文章最新草稿内容，AI 生成摘要建议
     *
     * @param userId 当前用户 ID（鉴权用）
     * @param articleId 文章 ID
     * @return AI 生成的摘要文本
     */
    String generateSummary(Long userId, Long articleId);
}
