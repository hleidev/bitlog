package top.harrylei.bitlog.article.service;

/**
 * 文章统计业务服务接口，供其他域更新文章维度的计数
 *
 * @author Harry
 * @since 2026-07-28
 */
public interface ArticleStatisticsService {

    /**
     * 增加评论数
     *
     * @param articleId 文章 ID
     * @param delta 增量，须为正数
     */
    void increaseCommentCount(Long articleId, int delta);

    /**
     * 减少评论数，结果不会低于 0
     *
     * @param articleId 文章 ID
     * @param delta 减量，须为正数
     */
    void decreaseCommentCount(Long articleId, int delta);
}
