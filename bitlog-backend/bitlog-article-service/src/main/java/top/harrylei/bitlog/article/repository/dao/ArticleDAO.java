package top.harrylei.bitlog.article.repository.dao;

import com.baomidou.mybatisplus.core.metadata.IPage;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.baomidou.mybatisplus.extension.service.impl.ServiceImpl;
import org.springframework.stereotype.Repository;
import top.harrylei.bitlog.api.enums.article.ArticleStatusEnum;
import top.harrylei.bitlog.api.model.article.query.ArticlePageQuery;
import top.harrylei.bitlog.article.repository.entity.ArticleDO;
import top.harrylei.bitlog.article.repository.mapper.ArticleMapper;
import top.harrylei.bitlog.common.enums.DeleteStatusEnum;

/**
 * 文章主表数据访问对象
 *
 * @author harry
 * @since 0.0.1
 */
@Repository
public class ArticleDAO extends ServiceImpl<ArticleMapper, ArticleDO> {

    public ArticleDO getByIdAndNotDeleted(Long articleId) {
        if (articleId == null) {
            return null;
        }
        return lambdaQuery()
                .eq(ArticleDO::getId, articleId)
                .eq(ArticleDO::getDeleted, DeleteStatusEnum.NOT_DELETED)
                .one();
    }

    /** 新建草稿后设置首个版本 ID 和版本计数 */
    public void updateAfterFirstSave(Long articleId, Long versionId) {
        lambdaUpdate()
                .eq(ArticleDO::getId, articleId)
                .set(ArticleDO::getLatestVersionId, versionId)
                .set(ArticleDO::getVersionCount, 1)
                .update();
    }

    /** 追加新版本后更新最新版本 ID 和版本计数 */
    public void updateLatestVersion(Long articleId, Long versionId) {
        lambdaUpdate()
                .eq(ArticleDO::getId, articleId)
                .set(ArticleDO::getLatestVersionId, versionId)
                .setSql("version_count = version_count + 1")
                .update();
    }

    /** 发布文章：更新封面、摘要、分类和已发布版本 ID */
    public void publish(Long articleId, String cover, String summary, Long categoryId, Long publishedVersionId) {
        lambdaUpdate()
                .eq(ArticleDO::getId, articleId)
                .set(ArticleDO::getCover, cover)
                .set(ArticleDO::getSummary, summary)
                .set(ArticleDO::getCategoryId, categoryId)
                .set(ArticleDO::getPublishedVersionId, publishedVersionId)
                .update();
    }

    /** 取消发布：清空已发布版本 ID */
    public void unpublish(Long articleId) {
        lambdaUpdate()
                .eq(ArticleDO::getId, articleId)
                .set(ArticleDO::getPublishedVersionId, null)
                .update();
    }

    /** 软删除文章 */
    public void delete(Long articleId) {
        lambdaUpdate()
                .eq(ArticleDO::getId, articleId)
                .set(ArticleDO::getDeleted, DeleteStatusEnum.DELETED)
                .update();
    }

    /** 分页查询已发布文章 */
    public IPage<ArticleDO> pagePublished(ArticlePageQuery query, Page<ArticleDO> page) {
        return lambdaQuery()
                .eq(ArticleDO::getDeleted, DeleteStatusEnum.NOT_DELETED)
                .isNotNull(ArticleDO::getPublishedVersionId)
                .eq(query.getUserId() != null, ArticleDO::getUserId, query.getUserId())
                .eq(query.getCategoryId() != null, ArticleDO::getCategoryId, query.getCategoryId())
                .page(page);
    }

    /** 分页查询用户文章（支持草稿/发布状态过滤） */
    public IPage<ArticleDO> pageByUser(Long userId, ArticlePageQuery query, Page<ArticleDO> page) {
        ArticleStatusEnum status = query.getStatus();
        return lambdaQuery()
                .eq(ArticleDO::getDeleted, DeleteStatusEnum.NOT_DELETED)
                .eq(ArticleDO::getUserId, userId)
                .isNotNull(ArticleStatusEnum.PUBLISHED == status, ArticleDO::getPublishedVersionId)
                .isNull(ArticleStatusEnum.DRAFT == status, ArticleDO::getPublishedVersionId)
                .eq(query.getCategoryId() != null, ArticleDO::getCategoryId, query.getCategoryId())
                .page(page);
    }
}
