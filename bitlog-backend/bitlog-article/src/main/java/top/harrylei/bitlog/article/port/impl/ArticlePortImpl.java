package top.harrylei.bitlog.article.port.impl;

import java.util.Collection;
import java.util.HashMap;
import java.util.Map;
import java.util.stream.Collectors;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import top.harrylei.bitlog.article.port.ArticlePort;
import top.harrylei.bitlog.article.repository.dao.ArticleDAO;
import top.harrylei.bitlog.article.repository.dao.ArticleStatisticsDAO;
import top.harrylei.bitlog.article.repository.dao.ArticleVersionDAO;
import top.harrylei.bitlog.article.repository.entity.ArticleDO;
import top.harrylei.bitlog.article.repository.entity.ArticleVersionDO;

/**
 * 文章模块对外契约实现
 *
 * @author Harry
 * @since 2026-08-09
 */
@Service
@RequiredArgsConstructor
public class ArticlePortImpl implements ArticlePort {

    private final ArticleDAO articleDAO;
    private final ArticleVersionDAO articleVersionDAO;
    private final ArticleStatisticsDAO articleStatisticsDAO;

    @Override
    public boolean isPublished(Long articleId) {
        if (articleId == null) {
            return false;
        }
        ArticleDO article = articleDAO.getByIdAndNotDeleted(articleId);
        return article != null && article.getPublishedVersionId() != null;
    }

    @Override
    public Map<Long, String> getArticleTitles(Collection<Long> articleIds) {
        if (articleIds == null || articleIds.isEmpty()) {
            return Map.of();
        }
        Map<Long, Long> versionIdByArticle = new HashMap<>();
        for (ArticleDO article : articleDAO.listByIdsAndNotDeleted(articleIds)) {
            Long versionId = article.getPublishedVersionId() != null
                    ? article.getPublishedVersionId()
                    : article.getLatestVersionId();
            if (versionId != null) {
                versionIdByArticle.put(article.getId(), versionId);
            }
        }
        if (versionIdByArticle.isEmpty()) {
            return Map.of();
        }

        Map<Long, String> titleByVersion =
                articleVersionDAO.listTitlesByVersionIds(versionIdByArticle.values()).stream()
                        .collect(Collectors.toMap(ArticleVersionDO::getId, ArticleVersionDO::getTitle));

        Map<Long, String> result = new HashMap<>();
        versionIdByArticle.forEach((articleId, versionId) -> {
            String title = titleByVersion.get(versionId);
            if (title != null) {
                result.put(articleId, title);
            }
        });
        return result;
    }

    @Override
    public Long getAuthorId(Long articleId) {
        return articleDAO.getAuthorId(articleId);
    }

    @Override
    public void increaseCommentCount(Long articleId, int delta) {
        articleStatisticsDAO.increaseCommentCount(articleId, delta);
    }

    @Override
    public void decreaseCommentCount(Long articleId, int delta) {
        articleStatisticsDAO.decreaseCommentCount(articleId, delta);
    }
}
