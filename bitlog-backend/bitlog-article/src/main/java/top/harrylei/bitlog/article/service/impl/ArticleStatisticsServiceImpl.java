package top.harrylei.bitlog.article.service.impl;

import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import top.harrylei.bitlog.article.repository.dao.ArticleStatisticsDAO;
import top.harrylei.bitlog.article.service.ArticleStatisticsService;

/**
 * 文章统计业务服务实现
 *
 * @author Harry
 * @since 2026-07-28
 */
@Service
@RequiredArgsConstructor
public class ArticleStatisticsServiceImpl implements ArticleStatisticsService {

    private final ArticleStatisticsDAO articleStatisticsDAO;

    @Override
    public void increaseCommentCount(Long articleId, int delta) {
        articleStatisticsDAO.increaseCommentCount(articleId, delta);
    }

    @Override
    public void decreaseCommentCount(Long articleId, int delta) {
        articleStatisticsDAO.decreaseCommentCount(articleId, delta);
    }
}
