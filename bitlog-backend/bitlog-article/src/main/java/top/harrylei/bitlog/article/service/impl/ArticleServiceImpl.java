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
import top.harrylei.bitlog.api.model.article.query.ArticlePageQuery;
import top.harrylei.bitlog.api.model.article.req.ArticlePublishRequest;
import top.harrylei.bitlog.api.model.article.req.ArticleSaveRequest;
import top.harrylei.bitlog.api.model.article.vo.ArticleCountVO;
import top.harrylei.bitlog.api.model.article.vo.ArticleDetailVO;
import top.harrylei.bitlog.api.model.article.vo.ArticleListVO;
import top.harrylei.bitlog.api.model.article.vo.ArticleVersionDetailVO;
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

import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.HashMap;
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
    private final FileUrlHelper fileUrlHelper;

    @Override
    @Transactional(rollbackFor = Exception.class)
    public Long saveArticle(Long userId, ArticleSaveRequest req) {
        // 创建文章主记录，cover/summary 初始为空字符串，发布时再填充
        ArticleDO article = new ArticleDO().setUserId(userId).setCover("").setSummary("").setTopping(0)
                .setVersionCount(0).setDeleted(DeleteStatusEnum.NOT_DELETED);
        articleDAO.save(article);

        // 创建第一个版本（仅保存标题和正文）
        ArticleVersionDO version = buildVersion(article.getId(), 1, req);
        articleVersionDAO.save(version);

        // 更新 latestVersionId 和版本计数
        articleDAO.updateAfterFirstSave(article.getId(), version.getId());

        // 初始化文章统计记录
        ArticleStatisticsDO stats = new ArticleStatisticsDO().setArticleId(article.getId()).setReadCount(0)
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
        int nextVersion = getNextVersion(articleId);
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

        // 更新文章主表：封面、摘要、分类、已发布版本；首次发布时写入 publishTime
        LocalDateTime publishTime = article.getPublishTime() != null ? article.getPublishTime() : LocalDateTime.now();
        articleDAO.publish(articleId, req.getCover() != null ? req.getCover() : "",
                req.getSummary() != null ? req.getSummary() : "", newCategoryId, article.getLatestVersionId(),
                publishTime);

        // 更新标签关联：删除旧的，保存新的
        articleTagDAO.removeByArticleId(articleId);
        saveArticleTags(articleId, newTagIds);

        // 更新标签文章计数（仅再次发布时才需回退旧计数；首次发布无已发布记录，跳过 decrement）
        if (article.getPublishedVersionId() != null) {
            tagDAO.decrementArticleCount(oldTagIds);
        }
        if (!newTagIds.isEmpty()) {
            tagDAO.incrementArticleCount(newTagIds);
        }

        // 更新分类文章计数（仅再次发布时才回退旧计数；首次发布或取消发布后重新发布只需增加新分类计数）
        if (article.getPublishedVersionId() != null) {
            if (!Objects.equals(oldCategoryId, newCategoryId)) {
                if (oldCategoryId != null) {
                    categoryDAO.decrementArticleCount(oldCategoryId);
                }
                if (newCategoryId != null) {
                    categoryDAO.incrementArticleCount(newCategoryId);
                }
            }
        } else {
            if (newCategoryId != null) {
                categoryDAO.incrementArticleCount(newCategoryId);
            }
        }

        log.info("发布文章 articleId={} categoryId={} tagCount={}", articleId, newCategoryId, newTagIds.size());
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public void updateStatus(Long userId, Long articleId, ArticleStatusEnum status) {
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
    public void batchUpdateStatus(Long userId, List<Long> articleIds, ArticleStatusEnum status) {
        articleIds.forEach(id -> updateStatus(userId, id, status));
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
                Map<Long, Long> tagCountMap = articleTags.stream()
                        .collect(Collectors.groupingBy(ArticleTagDO::getTagId, Collectors.counting()));
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
    public PageVO<ArticleVO> pagePublished(ArticlePageQuery query) {
        IPage<ArticleDO> page = articleDAO.pagePublished(query, buildPage(query));
        return toArticlePageVO(page, false, query);
    }

    @Override
    @Transactional(readOnly = true)
    public ArticleListVO pageMyArticles(Long userId, ArticlePageQuery query) {
        IPage<ArticleDO> page = articleDAO.pageByUser(userId, query, buildPage(query));
        long total = articleDAO.countByUser(userId);
        long published = articleDAO.countByUserAndStatus(userId, ArticleStatusEnum.PUBLISHED);
        ArticleCountVO counts = new ArticleCountVO().setTotal(total).setPublished(published)
                .setDraft(total - published);
        return new ArticleListVO().setCounts(counts).setPage(toArticlePageVO(page, true, query));
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
            dto.setTopping(a.getTopping());
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

    private record ArticleTagsData(Map<Long, List<String>> namesByArticle, Map<Long, List<Long>> idsByArticle) {
    }

    private ArticleTagsData buildTagsData(List<Long> articleIds) {
        if (articleIds == null || articleIds.isEmpty()) {
            return new ArticleTagsData(Map.of(), Map.of());
        }
        List<ArticleTagDO> articleTags = articleTagDAO
                .list(Wrappers.lambdaQuery(ArticleTagDO.class).in(ArticleTagDO::getArticleId, articleIds));
        if (articleTags.isEmpty()) {
            return new ArticleTagsData(Map.of(), Map.of());
        }
        Set<Long> tagIds = articleTags.stream().map(ArticleTagDO::getTagId).collect(Collectors.toSet());
        Map<Long, String> tagNameById = tagDAO.listByIds(tagIds).stream()
                .collect(Collectors.toMap(TagDO::getId, TagDO::getName));
        Map<Long, List<String>> namesByArticle = new HashMap<>();
        Map<Long, List<Long>> idsByArticle = new HashMap<>();
        for (ArticleTagDO at : articleTags) {
            namesByArticle.computeIfAbsent(at.getArticleId(), k -> new ArrayList<>())
                    .add(tagNameById.getOrDefault(at.getTagId(), ""));
            idsByArticle.computeIfAbsent(at.getArticleId(), k -> new ArrayList<>()).add(at.getTagId());
        }
        return new ArticleTagsData(namesByArticle, idsByArticle);
    }

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
        return new ArticleVersionDO().setArticleId(articleId).setVersion(version).setTitle(req.getTitle())
                .setContent(req.getContent());
    }

    private void saveArticleTags(Long articleId, List<Long> tagIds) {
        if (tagIds == null || tagIds.isEmpty()) {
            return;
        }
        List<ArticleTagDO> tagLinks = tagIds.stream()
                .map(tagId -> new ArticleTagDO().setArticleId(articleId).setTagId(tagId)).toList();
        articleTagDAO.saveBatch(tagLinks);
    }

    private ArticleDetailVO buildDetailVO(ArticleDO article, ArticleVersionDO version) {
        ArticleDetailVO vo = articleConverter.toDetailVO(article, version);
        vo.setCover(fileUrlHelper.buildUrl(vo.getCover()));
        vo.setStatus(article.getPublishedVersionId() != null ? ArticleStatusEnum.PUBLISHED : ArticleStatusEnum.DRAFT);
        vo.setTopping(article.getTopping());
        vo.setUpdateTime(article.getUpdateTime());
        vo.setPublishTime(article.getPublishTime());

        if (article.getCategoryId() != null) {
            CategoryDO category = categoryDAO.getById(article.getCategoryId());
            if (category != null) {
                vo.setCategoryName(category.getName());
            }
        }

        List<Long> tagIds = articleTagDAO.listTagIdsByArticleId(article.getId());
        vo.setTagIds(tagIds);
        if (!tagIds.isEmpty()) {
            vo.setTags(tagDAO.listByIds(tagIds).stream().map(TagDO::getName).toList());
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

        List<Long> versionIds = articles.stream().map(a -> isDraft ? a.getLatestVersionId() : a.getPublishedVersionId())
                .filter(Objects::nonNull).toList();
        Map<Long, ArticleVersionDO> versionMap = articleVersionDAO.listByVersionIds(versionIds).stream()
                .collect(Collectors.toMap(ArticleVersionDO::getId, Function.identity()));

        Set<Long> categoryIds = articles.stream().map(ArticleDO::getCategoryId).filter(Objects::nonNull)
                .collect(Collectors.toSet());
        Map<Long, String> categoryNameMap = categoryIds.isEmpty() ? Map.of() : categoryDAO.listByIds(categoryIds)
                .stream().collect(Collectors.toMap(CategoryDO::getId, CategoryDO::getName));

        List<Long> articleIds = articles.stream().map(ArticleDO::getId).toList();
        Map<Long, ArticleStatisticsDO> statsMap = articleStatisticsDAO.listByArticleIds(articleIds).stream()
                .collect(Collectors.toMap(ArticleStatisticsDO::getArticleId, Function.identity()));

        ArticleTagsData tagsData = buildTagsData(articleIds);

        List<ArticleVO> voList = articles.stream().map(a -> {
            Long vId = isDraft ? a.getLatestVersionId() : a.getPublishedVersionId();
            ArticleVersionDO v = vId != null ? versionMap.get(vId) : null;
            if (v == null) {
                return null;
            }

            ArticleVO vo = articleConverter.toVO(a, v);
            vo.setCover(fileUrlHelper.buildUrl(vo.getCover()));
            vo.setStatus(a.getPublishedVersionId() != null ? ArticleStatusEnum.PUBLISHED : ArticleStatusEnum.DRAFT);
            vo.setTopping(a.getTopping());
            vo.setUpdateTime(a.getUpdateTime());
            vo.setPublishTime(a.getPublishTime());
            if (a.getCategoryId() != null) {
                vo.setCategoryName(categoryNameMap.get(a.getCategoryId()));
            }
            vo.setTagIds(tagsData.idsByArticle().getOrDefault(a.getId(), List.of()));
            vo.setTags(tagsData.namesByArticle().getOrDefault(a.getId(), List.of()));
            ArticleStatisticsDO stats = statsMap.get(a.getId());
            if (stats != null) {
                vo.setReadCount(stats.getReadCount());
                vo.setCommentCount(stats.getCommentCount());
            }
            return vo;
        }).filter(Objects::nonNull).toList();

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
