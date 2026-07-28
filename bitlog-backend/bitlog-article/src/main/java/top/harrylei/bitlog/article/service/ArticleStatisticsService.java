package top.harrylei.bitlog.article.service;

/**
 * 文章统计业务服务接口，供其他域更新文章维度的计数
 *
 * @author Harry
 * @since 2026-07-28
 */
public interface ArticleStatisticsService {

    /**
     * 评论数加一
     *
     * @param articleId 文章 ID
     */
    void incrementCommentCount(Long articleId);

    /**
     * 评论数减一，已为 0 时不再递减
     *
     * @param articleId 文章 ID
     */
    void decrementCommentCount(Long articleId);
}
