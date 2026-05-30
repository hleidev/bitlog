package top.harrylei.bitlog.article.service.impl;

import com.baomidou.mybatisplus.core.metadata.IPage;
import com.baomidou.mybatisplus.core.toolkit.Wrappers;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import top.harrylei.bitlog.api.enums.article.ArticleStatusEnum;
import top.harrylei.bitlog.api.model.article.dto.ArticleDTO;
import top.harrylei.bitlog.api.model.article.query.ArticlePageParam;
import top.harrylei.bitlog.api.model.article.req.ArticlePublishParam;
import top.harrylei.bitlog.api.model.article.req.ArticleSaveParam;
import top.harrylei.bitlog.api.model.article.vo.ArticleCountVO;
import top.harrylei.bitlog.api.model.article.vo.ArticleDetailVO;
import top.harrylei.bitlog.api.model.article.vo.ArticleListVO;
import top.harrylei.bitlog.api.model.article.vo.ArticlePublicDetailVO;
import top.harrylei.bitlog.api.model.article.vo.ArticlePublicVO;
import top.harrylei.bitlog.api.model.article.vo.ArticleVersionDetailVO;
import top.harrylei.bitlog.api.model.article.vo.ArticleVersionVO;
import top.harrylei.bitlog.api.model.article.vo.ArticleVO;
import top.harrylei.bitlog.api.model.article.vo.CategoryVO;
import top.harrylei.bitlog.api.model.article.vo.TagVO;
import org.springframework.lang.NonNull;
import top.harrylei.bitlog.article.component.ArticleReadDedupe;
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
import top.harrylei.bitlog.common.context.ReqInfoContext;
import top.harrylei.bitlog.common.enums.DeleteStatusEnum;
import top.harrylei.bitlog.common.enums.ResultCode;
import top.harrylei.bitlog.common.model.PageVO;

import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Set;
import java.util.function.Function;
import java.util.stream.Collectors;

/**
 * 文章业务服务实现
 *
 * @author Harry
 * @since 2026-04-09
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
    private final ArticleReadDedupe articleReadDedupe;

    @Override
    @Transactional(rollbackFor = Exception.class)
    public Long saveArticle(Long userId, ArticleSaveParam req) {
        ArticleDO article = new ArticleDO().setUserId(userId).setSummary("").setVersionCount(0)
            .setDeleted(DeleteStatusEnum.NOT_DELETED);
        articleDAO.save(article);

        // 创建第一个版本（仅保存标题和正文）
        ArticleVersionDO version = buildVersion(article.getId(), 1, req);
        articleVersionDAO.save(version);

        // 更新 latestVersionId 和版本计数
        articleDAO.updateAfterFirstSave(article.getId(), version.getId());

        // 初始化文章统计记录
        ArticleStatisticsDO stats =
            new ArticleStatisticsDO().setArticleId(article.getId()).setReadCount(0).setCommentCount(0);
        articleStatisticsDAO.save(stats);

        log.info("新建文章草稿 articleId={} userId={}", article.getId(), userId);
        return article.getId();
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public void updateArticle(Long userId, Long articleId, ArticleSaveParam req) {
        ArticleDO article = getArticleOrThrow(articleId);
        checkOwner(article, userId);

        // 创建新版本（仅保存标题和正文）
        int nextVersion = getNextVersion(articleId);
        ArticleVersionDO version = buildVersion(articleId, nextVersion, req);
        articleVersionDAO.save(version);

        // 仅更新 latestVersionId 和版本计数，不触碰 cover/summary/category/tags
        articleDAO.updateLatestVersion(articleId, version.getId());

        log.info("更新文章草稿 articleId={} version={}", articleId, nextVersion);
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public void publishArticle(Long userId, Long articleId, ArticlePublishParam req) {
        ArticleDO article = getArticleOrThrow(articleId);
        checkOwner(article, userId);
        if (article.getLatestVersionId() == null) {
            ResultCode.ARTICLE_VERSION_NOT_EXISTS.throwException();
        }

        List<Long> oldTagIds = articleTagDAO.listTagIdsByArticleId(articleId);
        Long oldCategoryId = article.getCategoryId();

        List<Long> newTagIds = req.getTagIds() != null ? req.getTagIds() : List.of();
        Long newCategoryId = req.getCategoryId();

        LocalDateTime publishTime = article.getPublishTime() != null ? article.getPublishTime() : LocalDateTime.now();
        articleDAO.publish(articleId, req.getSummary() != null ? req.getSummary() : "", newCategoryId,
            article.getLatestVersionId(), publishTime);

        // 更新标签关联：删除旧的，保存新的
        articleTagDAO.removeByArticleId(articleId);
        saveArticleTags(articleId, newTagIds);

        boolean isRepublish = article.getPublishedVersionId() != null;
        updateTagCounts(isRepublish, oldTagIds, newTagIds);
        updateCategoryCount(isRepublish, oldCategoryId, newCategoryId);

        log.info("发布文章 articleId={} categoryId={} tagCount={}", articleId, newCategoryId, newTagIds.size());
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public void updateStatus(Long userId, Long articleId, ArticleStatusEnum status) {
        doUpdateStatus(userId, articleId, status);
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public void batchUpdateStatus(Long userId, List<Long> articleIds, ArticleStatusEnum status) {
        articleIds.forEach(id -> doUpdateStatus(userId, id, status));
    }

    private void doUpdateStatus(Long userId, Long articleId, ArticleStatusEnum status) {
        ArticleDO article = getArticleOrThrow(articleId);
        checkOwner(article, userId);
        boolean isPublished = article.getPublishedVersionId() != null;
        if (status == ArticleStatusEnum.PUBLISHED && !isPublished) {
            articleDAO.updatePublishedVersionId(articleId, article.getLatestVersionId());
            if (article.getPublishTime() == null) {
                articleDAO.setPublishTime(articleId, LocalDateTime.now());
            }
            List<Long> tagIds = articleTagDAO.listTagIdsByArticleId(articleId);
            tagDAO.incrementArticleCount(tagIds);
            if (article.getCategoryId() != null) {
                categoryDAO.incrementArticleCount(article.getCategoryId());
            }
            log.info("重新发布文章 articleId={}", articleId);
        } else if (status == ArticleStatusEnum.DRAFT && isPublished) {
            List<Long> tagIds = articleTagDAO.listTagIdsByArticleId(articleId);
            tagDAO.decrementArticleCount(tagIds);
            categoryDAO.decrementArticleCount(article.getCategoryId());
            articleDAO.unpublish(articleId);
            log.info("取消发布文章 articleId={}", articleId);
        }
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public void deleteVersions(Long userId, Long articleId, List<Long> versionIds) {
        ArticleDO article = getArticleOrThrow(articleId);
        checkOwner(article, userId);

        Set<Long> protectedIds = new HashSet<>();
        if (article.getLatestVersionId() != null) {
            protectedIds.add(article.getLatestVersionId());
        }
        if (article.getPublishedVersionId() != null) {
            protectedIds.add(article.getPublishedVersionId());
        }

        List<Long> toDeleteIds =
            articleVersionDAO.listByVersionIds(versionIds).stream().filter(v -> v.getArticleId().equals(articleId))
                .filter(v -> !protectedIds.contains(v.getId())).map(ArticleVersionDO::getId).toList();

        if (toDeleteIds.isEmpty()) {
            return;
        }

        articleVersionDAO.removeByIds(toDeleteIds);
        articleDAO.decrementVersionCount(articleId, toDeleteIds.size());
        log.info("批量删除文章版本 articleId={} count={}", articleId, toDeleteIds.size());
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public void batchDelete(Long userId, List<Long> articleIds) {
        List<ArticleDO> articles = articleDAO.listByIdsAndNotDeleted(articleIds);
        if (articles.isEmpty()) {
            return;
        }
        articles.forEach(a -> checkOwner(a, userId));

        List<ArticleDO> publishedArticles = articles.stream().filter(a -> a.getPublishedVersionId() != null).toList();
        if (!publishedArticles.isEmpty()) {
            List<Long> publishedIds = publishedArticles.stream().map(ArticleDO::getId).toList();
            List<ArticleTagDO> articleTags = articleTagDAO
                .list(Wrappers.lambdaQuery(ArticleTagDO.class).in(ArticleTagDO::getArticleId, publishedIds));
            if (!articleTags.isEmpty()) {
                Map<Long, Long> tagCountMap =
                    articleTags.stream().collect(Collectors.groupingBy(ArticleTagDO::getTagId, Collectors.counting()));
                tagDAO.decrementArticleCountBatch(tagCountMap);
            }
            Map<Long, Long> categoryCountMap = publishedArticles.stream().filter(a -> a.getCategoryId() != null)
                .collect(Collectors.groupingBy(ArticleDO::getCategoryId, Collectors.counting()));
            if (!categoryCountMap.isEmpty()) {
                categoryDAO.decrementArticleCountBatch(categoryCountMap);
            }
        }

        List<Long> existingIds = articles.stream().map(ArticleDO::getId).toList();
        articleDAO.batchDelete(existingIds);
        log.info("批量删除文章 articleIds={}", existingIds);
    }

    @Override
    public ArticlePublicDetailVO getPublishedDetail(Long articleId) {
        ArticleDO article = getArticleOrThrow(articleId);
        if (article.getPublishedVersionId() == null) {
            ResultCode.ARTICLE_NOT_PUBLISHED.throwException();
        }
        ArticleVersionDO version = articleVersionDAO.getVersionById(article.getPublishedVersionId());
        if (version == null) {
            ResultCode.ARTICLE_VERSION_NOT_EXISTS.throwException();
        }
        String clientIp = ReqInfoContext.getContext().getClientIp();
        if (articleReadDedupe.shouldCount(articleId, clientIp)) {
            articleStatisticsDAO.incrementReadCount(articleId);
        }
        return buildPublicDetailVO(article, version);
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
        List<ArticleVersionVO> vos = articleConverter.toVersionVOList(versions);
        Long latestVersionId = article.getLatestVersionId();
        vos.forEach(vo -> vo.setLatest(latestVersionId != null && latestVersionId.equals(vo.getId())));
        return vos;
    }

    @Override
    public ArticleVersionDetailVO getVersionDetail(Long userId, Long articleId, Long versionId) {
        ArticleDO article = getArticleOrThrow(articleId);
        checkOwner(article, userId);
        ArticleVersionDO version = articleVersionDAO.getVersionById(versionId);
        if (version == null || !version.getArticleId().equals(articleId)) {
            ResultCode.ARTICLE_VERSION_NOT_EXISTS.throwException();
        }
        return articleConverter.toVersionDetailVO(version);
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

        int nextVersion = getNextVersion(articleId);
        ArticleVersionDO newVersion = new ArticleVersionDO().setArticleId(articleId).setVersion(nextVersion)
            .setTitle(target.getTitle()).setContent(target.getContent());
        articleVersionDAO.save(newVersion);

        articleDAO.updateLatestVersion(articleId, newVersion.getId());
        log.info("回滚文章 articleId={} 至 versionId={} 新版本={}", articleId, versionId, nextVersion);
    }

    @Override
    @Transactional(readOnly = true)
    public PageVO<ArticlePublicVO> pagePublished(ArticlePageParam query) {
        IPage<ArticleDO> page = articleDAO.pagePublished(query, buildPage(query));
        return toPublicArticlePageVO(page);
    }

    @Override
    @Transactional(readOnly = true)
    public ArticleListVO pageMyArticles(Long userId, ArticlePageParam query) {
        IPage<ArticleDO> page = articleDAO.pageByUser(userId, query, buildPage(query));
        long total = articleDAO.countByUser(userId);
        long published = articleDAO.countByUserAndStatus(userId, ArticleStatusEnum.PUBLISHED);
        ArticleCountVO counts =
            new ArticleCountVO().setTotal(total).setPublished(published).setDraft(total - published);
        return new ArticleListVO().setCounts(counts).setPage(toArticlePageVO(page, true));
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
        List<ArticleDO> articles = articleDAO.listByIdsAndNotDeleted(articleIds).stream()
            .filter(a -> a.getPublishedVersionId() != null).toList();
        if (articles.isEmpty()) {
            return List.of();
        }
        List<Long> versionIds = articles.stream().map(ArticleDO::getPublishedVersionId).toList();
        Map<Long, ArticleVersionDO> versionMap = articleVersionDAO.listByVersionIds(versionIds).stream()
            .collect(Collectors.toMap(ArticleVersionDO::getId, Function.identity()));
        List<Long> ids = articles.stream().map(ArticleDO::getId).toList();
        Map<Long, ArticleStatisticsDO> statsMap = articleStatisticsDAO.listByArticleIds(ids).stream()
            .collect(Collectors.toMap(ArticleStatisticsDO::getArticleId, Function.identity()));
        return articles.stream().filter(a -> versionMap.containsKey(a.getPublishedVersionId())).map(a -> {
            ArticleVersionDO version = versionMap.get(a.getPublishedVersionId());
            ArticleDTO dto = articleConverter.toDTO(a, version);
            dto.setPublishTime(a.getPublishTime());
            ArticleStatisticsDO stats = statsMap.get(a.getId());
            if (stats != null) {
                dto.setReadCount(stats.getReadCount());
                dto.setCommentCount(stats.getCommentCount());
            }
            return dto;
        }).toList();
    }

    // ==================== 私有方法 ====================

    private int getNextVersion(Long articleId) {
        return articleVersionDAO.getMaxVersion(articleId) + 1;
    }

    private record ArticleTagsData(Map<Long, List<TagVO>> tagVOsByArticle) {
    }

    private record PageLoadData(Map<Long, ArticleVersionDO> versionMap, Map<Long, String> categoryNameMap,
        ArticleTagsData tagsData) {
    }

    private ArticleTagsData buildTagsData(List<Long> articleIds) {
        if (articleIds == null || articleIds.isEmpty()) {
            return new ArticleTagsData(Map.of());
        }
        List<ArticleTagDO> articleTags =
            articleTagDAO.list(Wrappers.lambdaQuery(ArticleTagDO.class).in(ArticleTagDO::getArticleId, articleIds));
        if (articleTags.isEmpty()) {
            return new ArticleTagsData(Map.of());
        }
        Set<Long> tagIds = articleTags.stream().map(ArticleTagDO::getTagId).collect(Collectors.toSet());
        Map<Long, String> tagNameById =
            tagDAO.listByIds(tagIds).stream().collect(Collectors.toMap(TagDO::getId, TagDO::getName));
        Map<Long, List<TagVO>> tagVOsByArticle = new HashMap<>();
        for (ArticleTagDO at : articleTags) {
            tagVOsByArticle.computeIfAbsent(at.getArticleId(), k -> new ArrayList<>())
                .add(new TagVO().setId(at.getTagId()).setName(tagNameById.getOrDefault(at.getTagId(), "")));
        }
        return new ArticleTagsData(tagVOsByArticle);
    }

    private PageLoadData loadPageData(List<ArticleDO> articles, boolean isDraft) {
        List<Long> versionIds = articles.stream().map(a -> isDraft ? a.getLatestVersionId() : a.getPublishedVersionId())
            .filter(Objects::nonNull).toList();
        Map<Long, ArticleVersionDO> versionMap = articleVersionDAO.listByVersionIds(versionIds).stream()
            .collect(Collectors.toMap(ArticleVersionDO::getId, Function.identity()));

        Set<Long> categoryIds =
            articles.stream().map(ArticleDO::getCategoryId).filter(Objects::nonNull).collect(Collectors.toSet());
        Map<Long, String> categoryNameMap = categoryIds.isEmpty() ? Map.of() : categoryDAO.listByIds(categoryIds)
            .stream().collect(Collectors.toMap(CategoryDO::getId, CategoryDO::getName));

        return new PageLoadData(versionMap, categoryNameMap,
            buildTagsData(articles.stream().map(ArticleDO::getId).toList()));
    }

    @NonNull
    private ArticleDO getArticleOrThrow(Long articleId) {
        ArticleDO article = articleDAO.getByIdAndNotDeleted(articleId);
        if (article == null) {
            throw ResultCode.ARTICLE_NOT_EXISTS.toException();
        }
        return article;
    }

    private void checkOwner(ArticleDO article, Long userId) {
        if (!article.getUserId().equals(userId)) {
            ResultCode.ARTICLE_NO_PERMISSION.throwException();
        }
    }

    private ArticleVersionDO buildVersion(Long articleId, int version, ArticleSaveParam req) {
        return new ArticleVersionDO().setArticleId(articleId).setVersion(version).setTitle(req.getTitle())
            .setContent(req.getContent());
    }

    private void updateTagCounts(boolean isRepublish, List<Long> oldTagIds, List<Long> newTagIds) {
        if (isRepublish) {
            tagDAO.decrementArticleCount(oldTagIds);
        }
        if (!newTagIds.isEmpty()) {
            tagDAO.incrementArticleCount(newTagIds);
        }
    }

    private void updateCategoryCount(boolean isRepublish, Long oldCategoryId, Long newCategoryId) {
        if (!isRepublish) {
            if (newCategoryId != null) {
                categoryDAO.incrementArticleCount(newCategoryId);
            }
            return;
        }
        if (Objects.equals(oldCategoryId, newCategoryId)) {
            return;
        }
        if (oldCategoryId != null) {
            categoryDAO.decrementArticleCount(oldCategoryId);
        }
        if (newCategoryId != null) {
            categoryDAO.incrementArticleCount(newCategoryId);
        }
    }

    private void saveArticleTags(Long articleId, List<Long> tagIds) {
        if (tagIds == null || tagIds.isEmpty()) {
            return;
        }
        List<ArticleTagDO> tagLinks =
            tagIds.stream().map(tagId -> new ArticleTagDO().setArticleId(articleId).setTagId(tagId)).toList();
        articleTagDAO.saveBatch(tagLinks);
    }

    private ArticleDetailVO buildDetailVO(ArticleDO article, ArticleVersionDO version) {
        ArticleDetailVO vo = articleConverter.toDetailVO(article, version);
        vo.setStatus(article.getPublishedVersionId() != null ? ArticleStatusEnum.PUBLISHED : ArticleStatusEnum.DRAFT);
        vo.setUpdateTime(article.getUpdateTime());
        vo.setPublishTime(article.getPublishTime());

        if (article.getCategoryId() != null) {
            CategoryDO category = categoryDAO.getById(article.getCategoryId());
            if (category != null) {
                vo.setCategory(new CategoryVO().setId(category.getId()).setName(category.getName()));
            }
        }

        List<Long> tagIds = articleTagDAO.listTagIdsByArticleId(article.getId());
        if (!tagIds.isEmpty()) {
            Map<Long, String> tagNameById =
                tagDAO.listByIds(tagIds).stream().collect(Collectors.toMap(TagDO::getId, TagDO::getName));
            vo.setTags(
                tagIds.stream().map(id -> new TagVO().setId(id).setName(tagNameById.getOrDefault(id, ""))).toList());
        } else {
            vo.setTags(List.of());
        }

        ArticleStatisticsDO stats = articleStatisticsDAO.getByArticleId(article.getId());
        if (stats != null) {
            vo.setReadCount(stats.getReadCount());
            vo.setCommentCount(stats.getCommentCount());
        }
        return vo;
    }

    private ArticlePublicDetailVO buildPublicDetailVO(ArticleDO article, ArticleVersionDO version) {
        ArticlePublicDetailVO vo = articleConverter.toPublicDetailVO(article, version);
        vo.setPublishTime(article.getPublishTime());

        if (article.getCategoryId() != null) {
            CategoryDO category = categoryDAO.getById(article.getCategoryId());
            if (category != null) {
                vo.setCategory(new CategoryVO().setId(category.getId()).setName(category.getName()));
            }
        }

        List<Long> tagIds = articleTagDAO.listTagIdsByArticleId(article.getId());
        if (!tagIds.isEmpty()) {
            Map<Long, String> tagNameById =
                tagDAO.listByIds(tagIds).stream().collect(Collectors.toMap(TagDO::getId, TagDO::getName));
            vo.setTags(
                tagIds.stream().map(id -> new TagVO().setId(id).setName(tagNameById.getOrDefault(id, ""))).toList());
        } else {
            vo.setTags(List.of());
        }

        return vo;
    }

    private Page<ArticleDO> buildPage(ArticlePageParam query) {
        return new Page<>(query.getPageNum(), query.getPageSize());
    }

    private PageVO<ArticleVO> toArticlePageVO(IPage<ArticleDO> page, boolean isDraft) {
        List<ArticleDO> articles = page.getRecords();
        if (articles.isEmpty()) {
            return PageVO.of(page, List.of());
        }

        PageLoadData data = loadPageData(articles, isDraft);
        List<Long> articleIds = articles.stream().map(ArticleDO::getId).toList();
        Map<Long, ArticleStatisticsDO> statsMap = articleStatisticsDAO.listByArticleIds(articleIds).stream()
            .collect(Collectors.toMap(ArticleStatisticsDO::getArticleId, Function.identity()));

        List<ArticleVO> voList = articles.stream().map(a -> {
            Long vId = isDraft ? a.getLatestVersionId() : a.getPublishedVersionId();
            ArticleVersionDO v = vId != null ? data.versionMap().get(vId) : null;
            if (v == null) {
                return null;
            }

            ArticleVO vo = articleConverter.toVO(a, v);
            vo.setStatus(a.getPublishedVersionId() != null ? ArticleStatusEnum.PUBLISHED : ArticleStatusEnum.DRAFT);
            vo.setUpdateTime(a.getUpdateTime());
            vo.setPublishTime(a.getPublishTime());
            if (a.getCategoryId() != null) {
                vo.setCategory(
                    new CategoryVO().setId(a.getCategoryId()).setName(data.categoryNameMap().get(a.getCategoryId())));
            }
            vo.setTags(data.tagsData().tagVOsByArticle().getOrDefault(a.getId(), List.of()));
            ArticleStatisticsDO stats = statsMap.get(a.getId());
            if (stats != null) {
                vo.setReadCount(stats.getReadCount());
                vo.setCommentCount(stats.getCommentCount());
            }
            return vo;
        }).filter(Objects::nonNull).toList();

        return PageVO.of(page, voList);
    }

    private PageVO<ArticlePublicVO> toPublicArticlePageVO(IPage<ArticleDO> page) {
        List<ArticleDO> articles = page.getRecords();
        if (articles.isEmpty()) {
            return PageVO.of(page, List.of());
        }

        PageLoadData data = loadPageData(articles, false);

        List<ArticlePublicVO> voList = articles.stream().map(a -> {
            ArticleVersionDO v = data.versionMap().get(a.getPublishedVersionId());
            if (v == null) {
                return null;
            }
            ArticlePublicVO vo = articleConverter.toPublicVO(a, v);
            vo.setPublishTime(a.getPublishTime());
            if (a.getCategoryId() != null) {
                vo.setCategory(
                    new CategoryVO().setId(a.getCategoryId()).setName(data.categoryNameMap().get(a.getCategoryId())));
            }
            vo.setTags(data.tagsData().tagVOsByArticle().getOrDefault(a.getId(), List.of()));
            return vo;
        }).filter(Objects::nonNull).toList();

        return PageVO.of(page, voList);
    }

}
