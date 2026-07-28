package top.harrylei.bitlog.article.service.impl;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import top.harrylei.bitlog.article.repository.dao.ArticleStatisticsDAO;
import top.harrylei.bitlog.article.service.ArticleStatisticsService;

/**
 * 文章统计业务服务实现
 *
 * @author Harry
 * @since 2026-07-28
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class ArticleStatisticsServiceImpl implements ArticleStatisticsService {

    private final ArticleStatisticsDAO articleStatisticsDAO;

    @Override
    public void incrementCommentCount(Long articleId) {
        articleStatisticsDAO.incrementCommentCount(articleId);
    }

    @Override
    public void decrementCommentCount(Long articleId) {
        articleStatisticsDAO.decrementCommentCount(articleId);
    }
}
