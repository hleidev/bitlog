package top.harrylei.bitlog.article.repository.dao;

import com.baomidou.mybatisplus.extension.service.impl.ServiceImpl;
import org.springframework.stereotype.Repository;
import top.harrylei.bitlog.article.repository.entity.ArticleVersionDO;
import top.harrylei.bitlog.article.repository.mapper.ArticleVersionMapper;

import java.util.Collection;
import java.util.List;

/**
 * 文章版本数据访问对象
 *
 * @author Harry
 * @since 2026-04-09
 */
@Repository
public class ArticleVersionDAO extends ServiceImpl<ArticleVersionMapper, ArticleVersionDO> {

    /**
     * 查询文章的最新版本号，不存在时返回 0
     *
     * @param articleId
     *            文章 ID
     * 
     * @return 最大版本号
     */
    public int getMaxVersion(Long articleId) {
        return getBaseMapper().getMaxVersion(articleId);
    }

    /**
     * 查询文章的所有版本列表（按版本号降序）
     *
     * @param articleId
     *            文章 ID
     * 
     * @return 版本列表
     */
    public List<ArticleVersionDO> listByArticleId(Long articleId) {
        return lambdaQuery().eq(ArticleVersionDO::getArticleId, articleId).orderByDesc(ArticleVersionDO::getVersion)
                .list();
    }

    /** 根据版本 ID 查询版本（版本记录无软删除） */
    public ArticleVersionDO getVersionById(Long versionId) {
        return getById(versionId);
    }

    /** 批量查询版本（版本记录无软删除） */
    public List<ArticleVersionDO> listByVersionIds(Collection<Long> versionIds) {
        return listByIds(versionIds);
    }
}
