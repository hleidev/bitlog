package top.harrylei.bitlog.article.repository.dao;

import com.baomidou.mybatisplus.extension.service.impl.ServiceImpl;
import org.springframework.stereotype.Repository;
import top.harrylei.bitlog.article.repository.entity.ArticleTagDO;
import top.harrylei.bitlog.article.repository.mapper.ArticleTagMapper;

import java.util.List;

/**
 * 文章标签关联数据访问对象
 *
 * @author Harry
 * @since 2026-04-09
 */
@Repository
public class ArticleTagDAO extends ServiceImpl<ArticleTagMapper, ArticleTagDO> {

    /**
     * 删除文章的所有标签关联
     *
     * @param articleId 文章 ID
     */
    public void removeByArticleId(Long articleId) {
        lambdaUpdate().eq(ArticleTagDO::getArticleId, articleId).remove();
    }

    /**
     * 删除指定标签的所有文章关联
     *
     * @param tagIds 标签 ID 列表
     */
    public void removeByTagIds(List<Long> tagIds) {
        if (tagIds == null || tagIds.isEmpty()) {
            return;
        }
        lambdaUpdate().in(ArticleTagDO::getTagId, tagIds).remove();
    }
}
