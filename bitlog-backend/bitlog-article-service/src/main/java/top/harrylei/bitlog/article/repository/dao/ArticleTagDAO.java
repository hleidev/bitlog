package top.harrylei.bitlog.article.repository.dao;

import com.baomidou.mybatisplus.extension.service.impl.ServiceImpl;
import org.springframework.stereotype.Repository;
import top.harrylei.bitlog.article.repository.entity.ArticleTagDO;
import top.harrylei.bitlog.article.repository.mapper.ArticleTagMapper;

import java.util.List;

/**
 * 文章标签关联数据访问对象
 *
 * @author harry
 * @since 0.0.1
 */
@Repository
public class ArticleTagDAO extends ServiceImpl<ArticleTagMapper, ArticleTagDO> {

    /**
     * 查询文章关联的所有标签 ID
     *
     * @param articleId 文章 ID
     * @return 标签 ID 列表
     */
    public List<Long> listTagIdsByArticleId(Long articleId) {
        return lambdaQuery()
                .eq(ArticleTagDO::getArticleId, articleId)
                .list()
                .stream()
                .map(ArticleTagDO::getTagId)
                .toList();
    }

    /**
     * 删除文章的所有标签关联
     *
     * @param articleId 文章 ID
     */
    public void removeByArticleId(Long articleId) {
        lambdaUpdate()
                .eq(ArticleTagDO::getArticleId, articleId)
                .remove();
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
        lambdaUpdate()
                .in(ArticleTagDO::getTagId, tagIds)
                .remove();
    }
}
