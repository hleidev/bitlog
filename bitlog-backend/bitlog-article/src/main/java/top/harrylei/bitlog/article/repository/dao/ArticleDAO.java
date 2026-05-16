package top.harrylei.bitlog.article.repository.dao;

import com.baomidou.mybatisplus.core.metadata.IPage;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.baomidou.mybatisplus.extension.service.impl.ServiceImpl;
import org.springframework.stereotype.Repository;
import org.springframework.util.StringUtils;
import top.harrylei.bitlog.api.enums.article.ArticleStatusEnum;
import top.harrylei.bitlog.api.model.article.query.ArticlePageQuery;
import top.harrylei.bitlog.article.repository.entity.ArticleDO;
import top.harrylei.bitlog.article.repository.mapper.ArticleMapper;
import top.harrylei.bitlog.common.enums.DeleteStatusEnum;

import java.time.LocalDateTime;
import java.util.Collection;
import java.util.List;

/**
 * 文章主表数据访问对象
 *
 * @author Harry
 * @since 2026-04-09
 */
@Repository
public class ArticleDAO extends ServiceImpl<ArticleMapper, ArticleDO> {

    public ArticleDO getByIdAndNotDeleted(Long articleId) {
        if (articleId == null) {
            return null;
        }
        return lambdaQuery().eq(ArticleDO::getId, articleId).eq(ArticleDO::getDeleted, DeleteStatusEnum.NOT_DELETED)
                .one();
    }

    /** 批量查询未删除文章 */
    public List<ArticleDO> listByIdsAndNotDeleted(Collection<Long> articleIds) {
        if (articleIds == null || articleIds.isEmpty()) {
            return List.of();
        }
        return lambdaQuery().in(ArticleDO::getId, articleIds).eq(ArticleDO::getDeleted, DeleteStatusEnum.NOT_DELETED)
                .list();
    }

    /** 新建草稿后设置首个版本 ID 和版本计数 */
    public void updateAfterFirstSave(Long articleId, Long versionId) {
        lambdaUpdate().eq(ArticleDO::getId, articleId).set(ArticleDO::getLatestVersionId, versionId)
                .set(ArticleDO::getVersionCount, 1).update();
    }

    /** 追加新版本后更新最新版本 ID 和版本计数 */
    public void updateLatestVersion(Long articleId, Long versionId) {
        lambdaUpdate().eq(ArticleDO::getId, articleId).set(ArticleDO::getLatestVersionId, versionId)
                .setSql("version_count = version_count + 1").update();
    }

    /** 发布文章：更新封面、摘要、分类、已发布版本 ID 和发布时间 */
    public void publish(Long articleId, String cover, String summary, Long categoryId, Long publishedVersionId,
            LocalDateTime publishTime) {
        lambdaUpdate().eq(ArticleDO::getId, articleId).set(ArticleDO::getCover, cover)
                .set(ArticleDO::getSummary, summary).set(ArticleDO::getCategoryId, categoryId)
                .set(ArticleDO::getPublishedVersionId, publishedVersionId).set(ArticleDO::getPublishTime, publishTime)
                .update();
    }

    /** 设置首次发布时间（仅在 publishTime 为 null 时由 updateStatus 重新发布路径调用） */
    public void setPublishTime(Long articleId, LocalDateTime publishTime) {
        lambdaUpdate().eq(ArticleDO::getId, articleId).set(ArticleDO::getPublishTime, publishTime).update();
    }

    /** 取消发布：清空已发布版本 ID */
    public void unpublish(Long articleId) {
        lambdaUpdate().eq(ArticleDO::getId, articleId).set(ArticleDO::getPublishedVersionId, null).update();
    }

    /** 重新发布：将 publishedVersionId 更新为指定版本 */
    public void updatePublishedVersionId(Long articleId, Long versionId) {
        lambdaUpdate().eq(ArticleDO::getId, articleId).set(ArticleDO::getPublishedVersionId, versionId).update();
    }

    /** 软删除文章 */
    public void delete(Long articleId) {
        lambdaUpdate().eq(ArticleDO::getId, articleId).set(ArticleDO::getDeleted, DeleteStatusEnum.DELETED).update();
    }

    /** 批量软删除文章 */
    public void batchDelete(List<Long> articleIds) {
        lambdaUpdate().in(ArticleDO::getId, articleIds).set(ArticleDO::getDeleted, DeleteStatusEnum.DELETED).update();
    }

    /** 统计用户全部未删除文章数 */
    public long countByUser(Long userId) {
        return lambdaQuery().eq(ArticleDO::getUserId, userId).eq(ArticleDO::getDeleted, DeleteStatusEnum.NOT_DELETED)
                .count();
    }

    /** 统计用户指定状态的文章数 */
    public long countByUserAndStatus(Long userId, ArticleStatusEnum status) {
        return lambdaQuery().eq(ArticleDO::getUserId, userId).eq(ArticleDO::getDeleted, DeleteStatusEnum.NOT_DELETED)
                .isNotNull(ArticleStatusEnum.PUBLISHED == status, ArticleDO::getPublishedVersionId)
                .isNull(ArticleStatusEnum.DRAFT == status, ArticleDO::getPublishedVersionId).count();
    }

    /** 分页查询已发布文章，关键词过滤下推到 SQL */
    public IPage<ArticleDO> pagePublished(ArticlePageQuery query, Page<ArticleDO> page) {
        return lambdaQuery().eq(ArticleDO::getDeleted, DeleteStatusEnum.NOT_DELETED)
                .isNotNull(ArticleDO::getPublishedVersionId)
                .eq(query.getUserId() != null, ArticleDO::getUserId, query.getUserId())
                .eq(query.getCategoryId() != null, ArticleDO::getCategoryId, query.getCategoryId())
                .apply(StringUtils.hasText(query.getKeyword()),
                        "EXISTS (SELECT 1 FROM article_version WHERE id = published_version_id AND title LIKE CONCAT('%', {0}, '%'))",
                        query.getKeyword())
                .page(page);
    }

    /** 分页查询用户文章（支持状态过滤），关键词过滤下推到 SQL */
    public IPage<ArticleDO> pageByUser(Long userId, ArticlePageQuery query, Page<ArticleDO> page) {
        ArticleStatusEnum status = query.getStatus();
        return lambdaQuery().eq(ArticleDO::getDeleted, DeleteStatusEnum.NOT_DELETED).eq(ArticleDO::getUserId, userId)
                .isNotNull(ArticleStatusEnum.PUBLISHED == status, ArticleDO::getPublishedVersionId)
                .isNull(ArticleStatusEnum.DRAFT == status, ArticleDO::getPublishedVersionId)
                .eq(query.getCategoryId() != null, ArticleDO::getCategoryId, query.getCategoryId())
                .apply(StringUtils.hasText(query.getKeyword()),
                        "EXISTS (SELECT 1 FROM article_version WHERE id = latest_version_id AND title LIKE CONCAT('%', {0}, '%'))",
                        query.getKeyword())
                .page(page);
    }
}
