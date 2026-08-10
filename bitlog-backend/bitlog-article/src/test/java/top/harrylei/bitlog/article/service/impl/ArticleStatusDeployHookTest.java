package top.harrylei.bitlog.article.service.impl;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import top.harrylei.bitlog.api.enums.article.ArticleStatusEnum;
import top.harrylei.bitlog.article.component.ArticleReadDedupe;
import top.harrylei.bitlog.article.component.DeployHookService;
import top.harrylei.bitlog.article.converter.ArticleConverter;
import top.harrylei.bitlog.article.repository.dao.ArticleDAO;
import top.harrylei.bitlog.article.repository.dao.ArticleStatisticsDAO;
import top.harrylei.bitlog.article.repository.dao.ArticleTagDAO;
import top.harrylei.bitlog.article.repository.dao.ArticleVersionDAO;
import top.harrylei.bitlog.article.repository.dao.CategoryDAO;
import top.harrylei.bitlog.article.repository.dao.TagDAO;
import top.harrylei.bitlog.article.repository.entity.ArticleDO;

import java.util.List;

import static org.mockito.ArgumentMatchers.anyLong;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

/**
 * 文章状态切换的部署钩子触发次数测试
 *
 * @author Harry
 * @since 2026-08-10
 */
@ExtendWith(MockitoExtension.class)
class ArticleStatusDeployHookTest {

    private static final Long USER_ID = 1L;

    @Mock
    private ArticleDAO articleDAO;
    @Mock
    private ArticleVersionDAO articleVersionDAO;
    @Mock
    private ArticleTagDAO articleTagDAO;
    @Mock
    private ArticleStatisticsDAO articleStatisticsDAO;
    @Mock
    private CategoryDAO categoryDAO;
    @Mock
    private TagDAO tagDAO;
    @Mock
    private ArticleConverter articleConverter;
    @Mock
    private ArticleReadDedupe articleReadDedupe;
    @Mock
    private DeployHookService deployHookService;

    @InjectMocks
    private ArticleServiceImpl articleService;

    @Test
    @DisplayName("批量下架三篇已发布文章，部署钩子只触发一次")
    void batchUpdateStatus_multiplePublishedArticles_triggersDeployOnce() {
        stubPublished(1L, 2L, 3L);

        articleService.batchUpdateStatus(USER_ID, List.of(1L, 2L, 3L), ArticleStatusEnum.DRAFT);

        verify(articleDAO, times(3)).unpublish(anyLong());
        verify(deployHookService, times(1)).triggerDeploy();
    }

    @Test
    @DisplayName("批量操作无一篇状态改变时，不触发部署钩子")
    void batchUpdateStatus_noStatusChanged_neverTriggersDeploy() {
        stubDraft(1L, 2L);

        articleService.batchUpdateStatus(USER_ID, List.of(1L, 2L), ArticleStatusEnum.DRAFT);

        verify(deployHookService, never()).triggerDeploy();
    }

    @Test
    @DisplayName("批量中仅部分文章状态改变，仍只触发一次")
    void batchUpdateStatus_partiallyChanged_triggersDeployOnce() {
        stubPublished(1L);
        stubDraft(2L);

        articleService.batchUpdateStatus(USER_ID, List.of(1L, 2L), ArticleStatusEnum.DRAFT);

        verify(deployHookService, times(1)).triggerDeploy();
    }

    @Test
    @DisplayName("单篇下架触发一次部署钩子")
    void updateStatus_publishedArticle_triggersDeployOnce() {
        stubPublished(1L);

        articleService.updateStatus(USER_ID, 1L, ArticleStatusEnum.DRAFT);

        verify(deployHookService, times(1)).triggerDeploy();
    }

    private void stubPublished(Long... articleIds) {
        for (Long id : articleIds) {
            when(articleDAO.getByIdAndNotDeleted(id))
                .thenReturn(new ArticleDO().setUserId(USER_ID).setPublishedVersionId(100L).setLatestVersionId(100L));
        }
    }

    private void stubDraft(Long... articleIds) {
        for (Long id : articleIds) {
            when(articleDAO.getByIdAndNotDeleted(id))
                .thenReturn(new ArticleDO().setUserId(USER_ID).setLatestVersionId(100L));
        }
    }

}
