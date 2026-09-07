package top.harrylei.bitlog.article.repository.dao;

import com.baomidou.mybatisplus.core.metadata.IPage;
import com.baomidou.mybatisplus.core.toolkit.Wrappers;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.baomidou.mybatisplus.extension.service.impl.ServiceImpl;
import java.time.OffsetDateTime;
import java.util.Collection;
import java.util.List;
import org.springframework.stereotype.Repository;
import top.harrylei.bitlog.article.model.dto.ArticleCountDTO;
import top.harrylei.bitlog.article.model.query.ArticlePageParam;
import top.harrylei.bitlog.article.model.query.MyArticlePageParam;
import top.harrylei.bitlog.article.repository.entity.ArticleDO;
import top.harrylei.bitlog.article.repository.mapper.ArticleMapper;
import top.harrylei.bitlog.common.enums.DeleteStatusEnum;

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
        return lambdaQuery()
                .eq(ArticleDO::getId, articleId)
                .eq(ArticleDO::getDeleted, DeleteStatusEnum.NOT_DELETED)
                .one();
    }

    /** 判断指定文章是否未删除且已发布 */
    public boolean existsPublishedById(Long articleId) {
        if (articleId == null) {
            return false;
        }
        return exists(Wrappers.lambdaQuery(ArticleDO.class)
                .eq(ArticleDO::getId, articleId)
                .isNotNull(ArticleDO::getPublishedVersionId)
                .eq(ArticleDO::getDeleted, DeleteStatusEnum.NOT_DELETED));
    }

    /** 查询未删除文章的作者 */
    public Long getAuthorId(Long articleId) {
        if (articleId == null) {
            return null;
        }
        ArticleDO article = lambdaQuery()
                .select(ArticleDO::getUserId)
                .eq(ArticleDO::getId, articleId)
                .eq(ArticleDO::getDeleted, DeleteStatusEnum.NOT_DELETED)
                .one();
        return article != null ? article.getUserId() : null;
    }

    /** 批量查询未删除文章 */
    public List<ArticleDO> listByIdsAndNotDeleted(Collection<Long> articleIds) {
        if (articleIds == null || articleIds.isEmpty()) {
            return List.of();
        }
        return lambdaQuery()
                .in(ArticleDO::getId, articleIds)
                .eq(ArticleDO::getDeleted, DeleteStatusEnum.NOT_DELETED)
                .list();
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

    /** 放弃未发布草稿：草稿头指针回退到指定版本，不新增版本，故不动版本计数 */
    public void resetLatestVersion(Long articleId, Long versionId) {
        lambdaUpdate()
                .eq(ArticleDO::getId, articleId)
                .set(ArticleDO::getLatestVersionId, versionId)
                .update();
    }

    /** 发布文章：更新摘要、分类、已发布版本 ID 和发布时间 */
    public void publish(
            Long articleId, String summary, Long categoryId, Long publishedVersionId, OffsetDateTime publishTime) {
        lambdaUpdate()
                .eq(ArticleDO::getId, articleId)
                .set(ArticleDO::getSummary, summary)
                .set(ArticleDO::getCategoryId, categoryId)
                .set(ArticleDO::getPublishedVersionId, publishedVersionId)
                .set(ArticleDO::getPublishTime, publishTime)
                .update();
    }

    /** 设置首次发布时间（仅在 publishTime 为 null 时由 updateStatus 重新发布路径调用） */
    public void setPublishTime(Long articleId, OffsetDateTime publishTime) {
        lambdaUpdate()
                .eq(ArticleDO::getId, articleId)
                .set(ArticleDO::getPublishTime, publishTime)
                .update();
    }

    /** 取消发布：清空已发布版本 ID */
    public void unpublish(Long articleId) {
        lambdaUpdate()
                .eq(ArticleDO::getId, articleId)
                .set(ArticleDO::getPublishedVersionId, null)
                .update();
    }

    /** 重新发布：将 publishedVersionId 更新为指定版本 */
    public void updatePublishedVersionId(Long articleId, Long versionId) {
        lambdaUpdate()
                .eq(ArticleDO::getId, articleId)
                .set(ArticleDO::getPublishedVersionId, versionId)
                .update();
    }

    /** 软删除文章 */
    public void delete(Long articleId) {
        lambdaUpdate()
                .eq(ArticleDO::getId, articleId)
                .set(ArticleDO::getDeleted, DeleteStatusEnum.DELETED)
                .update();
    }

    /** 删除版本后递减版本计数 */
    public void decrementVersionCount(Long articleId, int count) {
        lambdaUpdate()
                .eq(ArticleDO::getId, articleId)
                .setSql("version_count = version_count - " + count)
                .update();
    }

    /** 批量软删除文章 */
    public void batchDelete(List<Long> articleIds) {
        lambdaUpdate()
                .in(ArticleDO::getId, articleIds)
                .set(ArticleDO::getDeleted, DeleteStatusEnum.DELETED)
                .update();
    }

    /** 快速更新文章元数据（摘要、分类） */
    public void updateMeta(Long articleId, String summary, Long categoryId) {
        lambdaUpdate()
                .eq(ArticleDO::getId, articleId)
                .set(ArticleDO::getSummary, summary)
                .set(ArticleDO::getCategoryId, categoryId)
                .update();
    }

    /** 判断指定分类下是否存在已发布文章（用于删除前校验） */
    public boolean existsPublishedByCategory(Long categoryId) {
        return exists(Wrappers.lambdaQuery(ArticleDO.class)
                .eq(ArticleDO::getCategoryId, categoryId)
                .isNotNull(ArticleDO::getPublishedVersionId)
                .eq(ArticleDO::getDeleted, DeleteStatusEnum.NOT_DELETED));
    }

    /** 批量查询指定分类下的已发布文章（仅返回 categoryId，用于统计） */
    public List<ArticleDO> listPublishedByCategoryIds(List<Long> categoryIds) {
        if (categoryIds == null || categoryIds.isEmpty()) return List.of();
        return lambdaQuery()
                .in(ArticleDO::getCategoryId, categoryIds)
                .isNotNull(ArticleDO::getPublishedVersionId)
                .eq(ArticleDO::getDeleted, DeleteStatusEnum.NOT_DELETED)
                .select(ArticleDO::getCategoryId)
                .list();
    }

    /** 统计当前筛选下各状态的文章数，与 pageByUser 共用 XML 片段 */
    public ArticleCountDTO countStats(Long userId, MyArticlePageParam query) {
        return getBaseMapper().selectMyArticleStats(userId, query);
    }

    public IPage<ArticleDO> pagePublished(ArticlePageParam query, Page<ArticleDO> page) {
        return getBaseMapper().pagePublished(page, query);
    }

    /** 分页查询用户文章，排序由 BasePage 提供，DISPLAY_TIME 需要 SELECT 出的派生列故走 XML */
    public IPage<ArticleDO> pageByUser(Long userId, MyArticlePageParam query, Page<ArticleDO> page) {
        return getBaseMapper().pageByUser(page, userId, query);
    }
}
