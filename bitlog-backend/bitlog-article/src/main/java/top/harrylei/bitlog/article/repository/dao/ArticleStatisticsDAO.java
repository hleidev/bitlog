package top.harrylei.bitlog.article.repository.dao;

import com.baomidou.mybatisplus.extension.service.impl.ServiceImpl;
import org.springframework.stereotype.Repository;
import top.harrylei.bitlog.article.repository.entity.ArticleStatisticsDO;
import top.harrylei.bitlog.article.repository.mapper.ArticleStatisticsMapper;

import java.util.List;

/**
 * 文章统计数据访问对象
 *
 * @author Harry
 * @since 2026-04-09
 */
@Repository
public class ArticleStatisticsDAO extends ServiceImpl<ArticleStatisticsMapper, ArticleStatisticsDO> {

    /**
     * 根据文章 ID 查询统计信息
     *
     * @param articleId
     *            文章 ID
     * 
     * @return 统计信息
     */
    public ArticleStatisticsDO getByArticleId(Long articleId) {
        if (articleId == null) {
            return null;
        }
        return lambdaQuery().eq(ArticleStatisticsDO::getArticleId, articleId).one();
    }

    /**
     * 增加阅读计数
     *
     * @param articleId
     *            文章 ID
     */
    public void incrementReadCount(Long articleId) {
        if (articleId == null)
            return;
        lambdaUpdate().eq(ArticleStatisticsDO::getArticleId, articleId).setSql("read_count = read_count + 1").update();
    }

    /**
     * 增加评论计数
     *
     * @param articleId
     *            文章 ID
     * @param delta
     *            增量，须为正数
     */
    public void increaseCommentCount(Long articleId, int delta) {
        if (articleId == null || delta <= 0) {
            return;
        }
        lambdaUpdate().eq(ArticleStatisticsDO::getArticleId, articleId)
            .setSql("comment_count = comment_count + {0}", delta).update();
    }

    /**
     * 减少评论计数。comment_count 为无符号列，先转 SIGNED 再取 GREATEST，
     * 既避免相减下溢报错，也保证计数一旦漂移仍能收敛回 0 而非卡住
     *
     * @param articleId
     *            文章 ID
     * @param delta
     *            减量，须为正数
     */
    public void decreaseCommentCount(Long articleId, int delta) {
        if (articleId == null || delta <= 0) {
            return;
        }
        lambdaUpdate().eq(ArticleStatisticsDO::getArticleId, articleId)
            .setSql("comment_count = GREATEST(CAST(comment_count AS SIGNED) - {0}, 0)", delta).update();
    }

    /**
     * 批量查询文章统计信息
     *
     * @param articleIds
     *            文章 ID 列表
     * 
     * @return 统计信息列表
     */
    public List<ArticleStatisticsDO> listByArticleIds(List<Long> articleIds) {
        if (articleIds == null || articleIds.isEmpty()) {
            return List.of();
        }
        return lambdaQuery().in(ArticleStatisticsDO::getArticleId, articleIds).list();
    }
}
