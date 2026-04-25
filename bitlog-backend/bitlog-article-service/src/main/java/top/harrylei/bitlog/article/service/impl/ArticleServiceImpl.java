package top.harrylei.bitlog.article.service.impl;

import com.baomidou.mybatisplus.core.metadata.IPage;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.baomidou.mybatisplus.extension.plugins.pagination.PageDTO;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.util.StringUtils;
import top.harrylei.bitlog.api.model.article.dto.ArticleDTO;
import top.harrylei.bitlog.api.model.article.query.ArticlePageQuery;
import top.harrylei.bitlog.api.model.article.req.ArticlePublishRequest;
import top.harrylei.bitlog.api.model.article.req.ArticleSaveRequest;
import top.harrylei.bitlog.api.model.article.vo.ArticleDetailVO;
import top.harrylei.bitlog.api.model.article.vo.ArticleVersionVO;
import top.harrylei.bitlog.api.model.article.vo.ArticleVO;
import top.harrylei.bitlog.common.util.FileUrlHelper;
import top.harrylei.bitlog.article.converter.ArticleConverter;
import top.harrylei.bitlog.article.repository.dao.ArticleDAO;
import top.harrylei.bitlog.article.repository.dao.ArticleStatisticsDAO;
import top.harrylei.bitlog.article.repository.dao.ArticleTagDAO;
import top.harrylei.bitlog.article.repository.dao.ArticleVersionDAO;
import top.harrylei.bitlog.article.repository.dao.CategoryDAO;
import top.harrylei.bitlog.article.repository.dao.TagDAO;
import top.harrylei.bitlog.article.repository.entity.ArticleDO;
import top.harrylei.bitlog.article.repository.entity.ArticleStatisticsDO;
import top.harrylei.bitlog.article.repository.entity.ArticleTagDO;
import top.harrylei.bitlog.article.repository.entity.ArticleVersionDO;
import top.harrylei.bitlog.article.repository.entity.CategoryDO;
import top.harrylei.bitlog.article.repository.entity.TagDO;
import top.harrylei.bitlog.article.service.ArticleService;
import top.harrylei.bitlog.common.enums.DeleteStatusEnum;
import top.harrylei.bitlog.common.enums.ResultCode;
import top.harrylei.bitlog.common.model.PageVO;

import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Set;
import java.util.function.Function;
import java.util.stream.Collectors;

/**
 * 文章业务服务实现
 *
 * @author harry
 * @since 0.0.1
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class ArticleServiceImpl implements ArticleService {

    private final ArticleDAO articleDAO;
    private final ArticleVersionDAO articleVersionDAO;
    private final ArticleTagDAO articleTagDAO;
    private final ArticleStatisticsDAO articleStatisticsDAO;
    private final CategoryDAO categoryDAO;
    private final TagDAO tagDAO;
    private final ArticleConverter articleConverter;
    private final FileUrlHelper fileUrlHelper;

    @Override
    @Transactional(rollbackFor = Exception.class)
    public Long saveArticle(Long userId, ArticleSaveRequest req) {
        // 创建文章主记录，cover/summary 初始为空字符串，发布时再填充
        ArticleDO article = new ArticleDO()
                .setUserId(userId)
                .setCover("")
                .setSummary("")
                .setTopping(0)
                .setVersionCount(0)
                .setDeleted(DeleteStatusEnum.NOT_DELETED);
        articleDAO.save(article);

        // 创建第一个版本（仅保存标题和正文）
        ArticleVersionDO version = buildVersion(article.getId(), 1, req);
        articleVersionDAO.save(version);

        // 更新 latestVersionId 和版本计数
        articleDAO.updateAfterFirstSave(article.getId(), version.getId());

        // 初始化文章统计记录
        ArticleStatisticsDO stats = new ArticleStatisticsDO()
                .setArticleId(article.getId())
                .setReadCount(0)
                .setCommentCount(0);
        articleStatisticsDAO.save(stats);

        log.info("新建文章草稿 articleId={} userId={}", article.getId(), userId);
        return article.getId();
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public void updateArticle(Long userId, Long articleId, ArticleSaveRequest req) {
        ArticleDO article = getArticleOrThrow(articleId);
        checkOwner(article, userId);

        // 创建新版本（仅保存标题和正文）
        int nextVersion = articleVersionDAO.getMaxVersion(articleId) + 1;
        ArticleVersionDO version = buildVersion(articleId, nextVersion, req);
        articleVersionDAO.save(version);

        // 仅更新 latestVersionId 和版本计数，不触碰 cover/summary/category/tags
        articleDAO.updateLatestVersion(articleId, version.getId());

        log.info("更新文章草稿 articleId={} version={}", articleId, nextVersion);
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public void publishArticle(Long userId, Long articleId, ArticlePublishRequest req) {
        ArticleDO article = getArticleOrThrow(articleId);
        checkOwner(article, userId);
        if (article.getLatestVersionId() == null) {
            ResultCode.ARTICLE_VERSION_NOT_EXISTS.throwException();
        }

        // 获取旧的关联数据，用于更新计数
        List<Long> oldTagIds = articleTagDAO.listTagIdsByArticleId(articleId);
        Long oldCategoryId = article.getCategoryId();

        List<Long> newTagIds = req.getTagIds() != null ? req.getTagIds() : List.of();
        Long newCategoryId = req.getCategoryId();

        // 更新文章主表：封面、摘要、分类、已发布版本
        articleDAO.publish(articleId,
                req.getCover() != null ? req.getCover() : "",
                req.getSummary() != null ? req.getSummary() : "",
                newCategoryId,
                article.getLatestVersionId());

        // 更新标签关联：删除旧的，保存新的
        articleTagDAO.removeByArticleId(articleId);
        saveArticleTags(articleId, newTagIds);

        // 更新标签文章计数
        tagDAO.decrementArticleCount(oldTagIds);
        if (!newTagIds.isEmpty()) {
            tagDAO.incrementArticleCount(newTagIds);
        }

        // 更新分类文章计数
        if (oldCategoryId != null && !oldCategoryId.equals(newCategoryId)) {
            categoryDAO.decrementArticleCount(oldCategoryId);
            if (newCategoryId != null) {
                categoryDAO.incrementArticleCount(newCategoryId);
            }
        } else if (oldCategoryId == null && newCategoryId != null) {
            categoryDAO.incrementArticleCount(newCategoryId);
        }

        log.info("发布文章 articleId={} categoryId={} tagCount={}", articleId, newCategoryId, newTagIds.size());
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public void unpublishArticle(Long userId, Long articleId) {
        ArticleDO article = getArticleOrThrow(articleId);
        checkOwner(article, userId);
        if (article.getPublishedVersionId() == null) {
            return;
        }
        List<Long> tagIds = articleTagDAO.listTagIdsByArticleId(articleId);
        tagDAO.decrementArticleCount(tagIds);
        categoryDAO.decrementArticleCount(article.getCategoryId());
        articleDAO.unpublish(articleId);
        log.info("取消发布文章 articleId={}", articleId);
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public void deleteArticle(Long userId, Long articleId) {
        ArticleDO article = getArticleOrThrow(articleId);
        checkOwner(article, userId);

        // 只有已发布的文章才需要更新计数，草稿和已取消发布的文章不影响计数
        if (article.getPublishedVersionId() != null) {
            List<Long> tagIds = articleTagDAO.listTagIdsByArticleId(articleId);
            tagDAO.decrementArticleCount(tagIds);
            categoryDAO.decrementArticleCount(article.getCategoryId());
        }

        articleDAO.delete(articleId);
        log.info("删除文章 articleId={}", articleId);
    }

    @Override
    public ArticleDetailVO getPublishedDetail(Long articleId) {
        ArticleDO article = getArticleOrThrow(articleId);
        if (article.getPublishedVersionId() == null) {
            ResultCode.ARTICLE_NOT_PUBLISHED.throwException();
        }
        ArticleVersionDO version = articleVersionDAO.getVersionById(article.getPublishedVersionId());
        if (version == null) {
            ResultCode.ARTICLE_VERSION_NOT_EXISTS.throwException();
        }
        articleStatisticsDAO.incrementReadCount(articleId);
        return buildDetailVO(article, version);
    }

    @Override
    public ArticleDetailVO getDraftDetail(Long userId, Long articleId) {
        ArticleDO article = getArticleOrThrow(articleId);
        checkOwner(article, userId);
        if (article.getLatestVersionId() == null) {
            ResultCode.ARTICLE_VERSION_NOT_EXISTS.throwException();
        }
        ArticleVersionDO version = articleVersionDAO.getVersionById(article.getLatestVersionId());
        if (version == null) {
            ResultCode.ARTICLE_VERSION_NOT_EXISTS.throwException();
        }
        return buildDetailVO(article, version);
    }

    @Override
    public List<ArticleVersionVO> listVersions(Long userId, Long articleId) {
        ArticleDO article = getArticleOrThrow(articleId);
        checkOwner(article, userId);
        List<ArticleVersionDO> versions = articleVersionDAO.listByArticleId(articleId);
        return articleConverter.toVersionVOList(versions);
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public void rollbackVersion(Long userId, Long articleId, Long versionId) {
        ArticleDO article = getArticleOrThrow(articleId);
        checkOwner(article, userId);

        ArticleVersionDO target = articleVersionDAO.getVersionById(versionId);
        if (target == null || !target.getArticleId().equals(articleId)) {
            ResultCode.ARTICLE_VERSION_NOT_EXISTS.throwException();
        }

        int nextVersion = articleVersionDAO.getMaxVersion(articleId) + 1;
        ArticleVersionDO newVersion = new ArticleVersionDO()
                .setArticleId(articleId)
                .setVersion(nextVersion)
                .setTitle(target.getTitle())
                .setContent(target.getContent());
        articleVersionDAO.save(newVersion);

        articleDAO.updateLatestVersion(articleId, newVersion.getId());
        log.info("回滚文章 articleId={} 至 versionId={} 新版本={}", articleId, versionId, nextVersion);
    }

    @Override
    public PageVO<ArticleVO> pagePublished(ArticlePageQuery query) {
        IPage<ArticleDO> page = articleDAO.pagePublished(query, buildPage(query));
        return toArticlePageVO(page, false, query);
    }

    @Override
    public PageVO<ArticleVO> pageMyArticles(Long userId, ArticlePageQuery query) {
        IPage<ArticleDO> page = articleDAO.pageByUser(userId, query, buildPage(query));
        return toArticlePageVO(page, true, query);
    }

    @Override
    public ArticleDTO getArticleDTO(Long articleId) {
        ArticleDO article = getArticleOrThrow(articleId);
        if (article.getPublishedVersionId() == null) {
            return null;
        }
        ArticleVersionDO version = articleVersionDAO.getVersionById(article.getPublishedVersionId());
        if (version == null) {
            return null;
        }
        ArticleDTO dto = articleConverter.toDTO(article, version);
        dto.setTopping(article.getTopping());
        ArticleStatisticsDO stats = articleStatisticsDAO.getByArticleId(articleId);
        if (stats != null) {
            dto.setReadCount(stats.getReadCount());
            dto.setCommentCount(stats.getCommentCount());
        }
        return dto;
    }

    @Override
    public List<ArticleDTO> getArticleDTOBatch(List<Long> articleIds) {
        if (articleIds == null || articleIds.isEmpty()) {
            return List.of();
        }
        return articleIds.stream()
                .map(this::getArticleDTO)
                .filter(Objects::nonNull)
                .toList();
    }

    // ==================== 私有方法 ====================

    private ArticleDO getArticleOrThrow(Long articleId) {
        ArticleDO article = articleDAO.getByIdAndNotDeleted(articleId);
        if (article == null) {
            ResultCode.ARTICLE_NOT_EXISTS.throwException();
        }
        return article;
    }

    private void checkOwner(ArticleDO article, Long userId) {
        if (!article.getUserId().equals(userId)) {
            ResultCode.ARTICLE_NO_PERMISSION.throwException();
        }
    }

    private ArticleVersionDO buildVersion(Long articleId, int version, ArticleSaveRequest req) {
        return new ArticleVersionDO()
                .setArticleId(articleId)
                .setVersion(version)
                .setTitle(req.getTitle())
                .setContent(req.getContent());
    }

    private void saveArticleTags(Long articleId, List<Long> tagIds) {
        if (tagIds == null || tagIds.isEmpty()) {
            return;
        }
        List<ArticleTagDO> tagLinks = tagIds.stream()
                .map(tagId -> new ArticleTagDO().setArticleId(articleId).setTagId(tagId))
                .toList();
        articleTagDAO.saveBatch(tagLinks);
    }

    private ArticleDetailVO buildDetailVO(ArticleDO article, ArticleVersionDO version) {
        ArticleDetailVO vo = articleConverter.toDetailVO(article, version);
        vo.setCover(fileUrlHelper.buildUrl(vo.getCover()));
        vo.setTopping(article.getTopping());
        vo.setUpdateTime(article.getUpdateTime());

        if (article.getPublishedVersionId() != null) {
            vo.setPublishTime(version.getCreateTime());
        }

        if (article.getCategoryId() != null) {
            CategoryDO category = categoryDAO.getById(article.getCategoryId());
            if (category != null) {
                vo.setCategoryName(category.getName());
            }
        }

        List<Long> tagIds = articleTagDAO.listTagIdsByArticleId(article.getId());
        if (!tagIds.isEmpty()) {
            List<TagDO> tags = tagDAO.listByIds(tagIds);
            vo.setTags(tags.stream().map(TagDO::getName).toList());
        }

        ArticleStatisticsDO stats = articleStatisticsDAO.getByArticleId(article.getId());
        if (stats != null) {
            vo.setReadCount(stats.getReadCount());
            vo.setCommentCount(stats.getCommentCount());
        }
        return vo;
    }

    private Page<ArticleDO> buildPage(ArticlePageQuery query) {
        Page<ArticleDO> page = new Page<>(query.getPageNum(), query.getPageSize());
        page.addOrder(com.baomidou.mybatisplus.core.metadata.OrderItem.desc("create_time"));
        return page;
    }

    private PageVO<ArticleVO> toArticlePageVO(IPage<ArticleDO> page, boolean isDraft, ArticlePageQuery query) {
        List<ArticleDO> articles = page.getRecords();
        if (articles.isEmpty()) {
            PageVO<ArticleVO> empty = new PageVO<>();
            empty.setPageNum(page.getCurrent());
            empty.setPageSize(page.getSize());
            empty.setTotalElements(page.getTotal());
            empty.setTotalPages(page.getPages());
            empty.setHasPrevious(page.getCurrent() > 1);
            empty.setHasNext(page.getCurrent() < page.getPages());
            empty.setContent(List.of());
            return empty;
        }

        List<Long> versionIds = articles.stream()
                .map(a -> isDraft ? a.getLatestVersionId() : a.getPublishedVersionId())
                .filter(Objects::nonNull)
                .toList();
        Map<Long, ArticleVersionDO> versionMap = articleVersionDAO.listByVersionIds(versionIds).stream()
                .collect(Collectors.toMap(ArticleVersionDO::getId, Function.identity()));

        Set<Long> categoryIds = articles.stream()
                .map(ArticleDO::getCategoryId)
                .filter(Objects::nonNull)
                .collect(Collectors.toSet());
        Map<Long, String> categoryNameMap = categoryIds.isEmpty() ? Map.of() :
                categoryDAO.listByIds(categoryIds).stream()
                        .collect(Collectors.toMap(CategoryDO::getId, CategoryDO::getName));

        List<Long> articleIds = articles.stream().map(ArticleDO::getId).toList();
        Map<Long, ArticleStatisticsDO> statsMap = articleStatisticsDAO.listByArticleIds(articleIds).stream()
                .collect(Collectors.toMap(ArticleStatisticsDO::getArticleId, Function.identity()));

        String keyword = query.getKeyword();

        List<ArticleVO> voList = articles.stream()
                .map(a -> {
                    Long vId = isDraft ? a.getLatestVersionId() : a.getPublishedVersionId();
                    ArticleVersionDO v = vId != null ? versionMap.get(vId) : null;
                    if (v == null) {
                        return null;
                    }
                    if (StringUtils.hasText(keyword) && !v.getTitle().contains(keyword)) {
                        return null;
                    }

                    ArticleVO vo = articleConverter.toVO(a, v);
                    vo.setCover(fileUrlHelper.buildUrl(vo.getCover()));
                    vo.setTopping(a.getTopping());
                    vo.setUpdateTime(a.getUpdateTime());
                    if (a.getPublishedVersionId() != null) {
                        vo.setPublishTime(v.getCreateTime());
                    }
                    if (a.getCategoryId() != null) {
                        vo.setCategoryName(categoryNameMap.get(a.getCategoryId()));
                    }
                    ArticleStatisticsDO stats = statsMap.get(a.getId());
                    if (stats != null) {
                        vo.setReadCount(stats.getReadCount());
                        vo.setCommentCount(stats.getCommentCount());
                    }
                    return vo;
                })
                .filter(Objects::nonNull)
                .toList();

        PageVO<ArticleVO> pageVO = new PageVO<>();
        pageVO.setPageNum(page.getCurrent());
        pageVO.setPageSize(page.getSize());
        pageVO.setTotalElements(page.getTotal());
        pageVO.setTotalPages(page.getPages());
        pageVO.setHasPrevious(page.getCurrent() > 1);
        pageVO.setHasNext(page.getCurrent() < page.getPages());
        pageVO.setContent(voList);
        return pageVO;
    }

}
