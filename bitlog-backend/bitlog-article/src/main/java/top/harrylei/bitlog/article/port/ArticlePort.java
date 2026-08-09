package top.harrylei.bitlog.article.port;

import java.util.Collection;
import java.util.Map;

/**
 * 文章模块对外契约，其余模块只应依赖本接口，不直接注入领域服务
 *
 * @author Harry
 * @since 2026-08-09
 */
public interface ArticlePort {

    /**
     * 判断文章当前是否可被读者访问，不加载正文
     *
     * @param articleId 文章 ID
     * @return true 文章存在、未删除且处于已发布状态
     */
    boolean isPublished(Long articleId);

    /**
     * 批量查询文章标题，不加载正文，未发布文章回退到最新草稿版本的标题
     *
     * @param articleIds 文章 ID 集合
     * @return 文章 ID 到标题的映射，已删除文章不在结果中
     */
    Map<Long, String> getArticleTitles(Collection<Long> articleIds);

    /**
     * 增加文章评论数
     *
     * @param articleId 文章 ID
     * @param delta 增量
     */
    void increaseCommentCount(Long articleId, int delta);

    /**
     * 减少文章评论数
     *
     * @param articleId 文章 ID
     * @param delta 减量
     */
    void decreaseCommentCount(Long articleId, int delta);
}
