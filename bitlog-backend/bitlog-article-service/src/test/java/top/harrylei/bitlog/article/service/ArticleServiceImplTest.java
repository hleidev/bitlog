package top.harrylei.bitlog.article.service;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import top.harrylei.bitlog.api.model.article.dto.ArticleDTO;
import top.harrylei.bitlog.api.model.article.req.ArticlePublishRequest;
import top.harrylei.bitlog.api.model.article.req.ArticleSaveRequest;
import top.harrylei.bitlog.api.model.article.vo.ArticleDetailVO;
import top.harrylei.bitlog.api.model.article.vo.ArticleVersionVO;
import top.harrylei.bitlog.article.converter.ArticleConverter;
import top.harrylei.bitlog.article.repository.dao.ArticleDAO;
import top.harrylei.bitlog.article.repository.dao.ArticleStatisticsDAO;
import top.harrylei.bitlog.article.repository.dao.ArticleTagDAO;
import top.harrylei.bitlog.article.repository.dao.ArticleVersionDAO;
import top.harrylei.bitlog.article.repository.dao.CategoryDAO;
import top.harrylei.bitlog.article.repository.dao.TagDAO;
import top.harrylei.bitlog.article.repository.entity.ArticleDO;
import top.harrylei.bitlog.article.repository.entity.ArticleStatisticsDO;
import top.harrylei.bitlog.article.repository.entity.ArticleVersionDO;
import top.harrylei.bitlog.article.service.impl.ArticleServiceImpl;
import top.harrylei.bitlog.common.enums.DeleteStatusEnum;
import top.harrylei.bitlog.common.exception.BusinessException;

import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyList;
import static org.mockito.ArgumentMatchers.anyLong;
import static org.mockito.Mockito.doAnswer;
import static org.mockito.Mockito.doNothing;
import static org.mockito.Mockito.lenient;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
@DisplayName("ArticleServiceImpl 文章服务测试")
class ArticleServiceImplTest {

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

    @InjectMocks
    private ArticleServiceImpl articleService;

    @BeforeEach
    void setUp() {
        // lambdaUpdate() 链式调用默认返回 null 会 NPE，用 lenient 避免严格校验
        // 各测试按需自行 stub
    }

    // ==================== saveArticle ====================

    @Test
    @DisplayName("saveArticle_正常保存草稿_返回articleId")
    void saveArticle_saveDraft_returnsArticleId() {
        doAnswer(invocation -> {
            ArticleDO arg = invocation.getArgument(0);
            arg.setId(100L);
            return true;
        }).when(articleDAO).save(any(ArticleDO.class));

        doAnswer(invocation -> {
            ArticleVersionDO arg = invocation.getArgument(0);
            arg.setId(200L);
            return true;
        }).when(articleVersionDAO).save(any(ArticleVersionDO.class));

        stubArticleLambdaUpdate();
        when(articleStatisticsDAO.save(any())).thenReturn(true);

        ArticleSaveRequest req = buildSaveRequest();
        Long result = articleService.saveArticle(1L, req);

        assertThat(result).isEqualTo(100L);
        verify(articleDAO).save(any(ArticleDO.class));
        verify(articleVersionDAO).save(any(ArticleVersionDO.class));
    }

    // ==================== updateArticle ====================

    @Test
    @DisplayName("updateArticle_文章不存在_抛出BusinessException")
    void updateArticle_articleNotExists_throwsBusinessException() {
        when(articleDAO.getByIdAndNotDeleted(1L)).thenReturn(null);

        assertThatThrownBy(() -> articleService.updateArticle(1L, 1L, buildSaveRequest()))
                .isInstanceOf(BusinessException.class)
                .hasMessageContaining("文章不存在");
    }

    @Test
    @DisplayName("updateArticle_非作者操作_抛出BusinessException")
    void updateArticle_notOwner_throwsBusinessException() {
        ArticleDO article = buildArticle(1L, 999L);
        when(articleDAO.getByIdAndNotDeleted(1L)).thenReturn(article);

        assertThatThrownBy(() -> articleService.updateArticle(1L, 1L, buildSaveRequest()))
                .isInstanceOf(BusinessException.class)
                .hasMessageContaining("无权操作该文章");
    }

    // ==================== publishArticle ====================

    @Test
    @DisplayName("publishArticle_文章不存在_抛出BusinessException")
    void publishArticle_articleNotExists_throwsBusinessException() {
        when(articleDAO.getByIdAndNotDeleted(1L)).thenReturn(null);

        assertThatThrownBy(() -> articleService.publishArticle(1L, 1L, new ArticlePublishRequest()))
                .isInstanceOf(BusinessException.class)
                .hasMessageContaining("文章不存在");
    }

    @Test
    @DisplayName("publishArticle_非作者操作_抛出BusinessException")
    void publishArticle_notOwner_throwsBusinessException() {
        ArticleDO article = buildArticle(1L, 999L);
        when(articleDAO.getByIdAndNotDeleted(1L)).thenReturn(article);

        assertThatThrownBy(() -> articleService.publishArticle(1L, 1L, new ArticlePublishRequest()))
                .isInstanceOf(BusinessException.class)
                .hasMessageContaining("无权操作该文章");
    }

    @Test
    @DisplayName("publishArticle_latestVersionId为null_抛出BusinessException")
    void publishArticle_latestVersionIdNull_throwsBusinessException() {
        ArticleDO article = buildArticle(1L, 1L);
        article.setLatestVersionId(null);
        when(articleDAO.getByIdAndNotDeleted(1L)).thenReturn(article);

        assertThatThrownBy(() -> articleService.publishArticle(1L, 1L, new ArticlePublishRequest()))
                .isInstanceOf(BusinessException.class)
                .hasMessageContaining("文章版本不存在");
    }

    // ==================== unpublishArticle ====================

    @Test
    @DisplayName("unpublishArticle_文章不存在_抛出BusinessException")
    void unpublishArticle_articleNotExists_throwsBusinessException() {
        when(articleDAO.getByIdAndNotDeleted(1L)).thenReturn(null);

        assertThatThrownBy(() -> articleService.unpublishArticle(1L, 1L))
                .isInstanceOf(BusinessException.class)
                .hasMessageContaining("文章不存在");
    }

    @Test
    @DisplayName("unpublishArticle_非作者操作_抛出BusinessException")
    void unpublishArticle_notOwner_throwsBusinessException() {
        ArticleDO article = buildArticle(1L, 999L);
        when(articleDAO.getByIdAndNotDeleted(1L)).thenReturn(article);

        assertThatThrownBy(() -> articleService.unpublishArticle(1L, 1L))
                .isInstanceOf(BusinessException.class)
                .hasMessageContaining("无权操作该文章");
    }

    // ==================== deleteArticle ====================

    @Test
    @DisplayName("deleteArticle_文章不存在_抛出BusinessException")
    void deleteArticle_articleNotExists_throwsBusinessException() {
        when(articleDAO.getByIdAndNotDeleted(1L)).thenReturn(null);

        assertThatThrownBy(() -> articleService.deleteArticle(1L, 1L))
                .isInstanceOf(BusinessException.class)
                .hasMessageContaining("文章不存在");
    }

    @Test
    @DisplayName("deleteArticle_非作者操作_抛出BusinessException")
    void deleteArticle_notOwner_throwsBusinessException() {
        ArticleDO article = buildArticle(1L, 999L);
        when(articleDAO.getByIdAndNotDeleted(1L)).thenReturn(article);

        assertThatThrownBy(() -> articleService.deleteArticle(1L, 1L))
                .isInstanceOf(BusinessException.class)
                .hasMessageContaining("无权操作该文章");
    }

    // ==================== getPublishedDetail ====================

    @Test
    @DisplayName("getPublishedDetail_文章不存在_抛出BusinessException")
    void getPublishedDetail_articleNotExists_throwsBusinessException() {
        when(articleDAO.getByIdAndNotDeleted(1L)).thenReturn(null);

        assertThatThrownBy(() -> articleService.getPublishedDetail(1L))
                .isInstanceOf(BusinessException.class)
                .hasMessageContaining("文章不存在");
    }

    @Test
    @DisplayName("getPublishedDetail_文章未发布_抛出BusinessException")
    void getPublishedDetail_articleNotPublished_throwsBusinessException() {
        ArticleDO article = buildArticle(1L, 1L);
        article.setPublishedVersionId(null);
        when(articleDAO.getByIdAndNotDeleted(1L)).thenReturn(article);

        assertThatThrownBy(() -> articleService.getPublishedDetail(1L))
                .isInstanceOf(BusinessException.class)
                .hasMessageContaining("文章未发布");
    }

    @Test
    @DisplayName("getPublishedDetail_正常返回_detailVO不为null")
    void getPublishedDetail_withPublishedArticle_returnsDetailVO() {
        ArticleDO article = buildPublishedArticle(1L, 1L, 200L);
        ArticleVersionDO version = buildVersion(200L, 1L);
        ArticleDetailVO expectedVO = new ArticleDetailVO();

        when(articleDAO.getByIdAndNotDeleted(1L)).thenReturn(article);
        when(articleVersionDAO.getVersionById(200L)).thenReturn(version);
        when(articleConverter.toDetailVO(any(), any())).thenReturn(expectedVO);
        when(articleTagDAO.listTagIdsByArticleId(anyLong())).thenReturn(List.of());
        when(articleStatisticsDAO.getByArticleId(anyLong())).thenReturn(null);

        ArticleDetailVO result = articleService.getPublishedDetail(1L);

        assertThat(result).isNotNull();
    }

    // ==================== getDraftDetail ====================

    @Test
    @DisplayName("getDraftDetail_文章不存在_抛出BusinessException")
    void getDraftDetail_articleNotExists_throwsBusinessException() {
        when(articleDAO.getByIdAndNotDeleted(1L)).thenReturn(null);

        assertThatThrownBy(() -> articleService.getDraftDetail(1L, 1L))
                .isInstanceOf(BusinessException.class)
                .hasMessageContaining("文章不存在");
    }

    @Test
    @DisplayName("getDraftDetail_非作者操作_抛出BusinessException")
    void getDraftDetail_notOwner_throwsBusinessException() {
        ArticleDO article = buildArticle(1L, 999L);
        when(articleDAO.getByIdAndNotDeleted(1L)).thenReturn(article);

        assertThatThrownBy(() -> articleService.getDraftDetail(1L, 1L))
                .isInstanceOf(BusinessException.class)
                .hasMessageContaining("无权操作该文章");
    }

    @Test
    @DisplayName("getDraftDetail_latestVersionId为null_抛出BusinessException")
    void getDraftDetail_latestVersionIdNull_throwsBusinessException() {
        ArticleDO article = buildArticle(1L, 1L);
        article.setLatestVersionId(null);
        when(articleDAO.getByIdAndNotDeleted(1L)).thenReturn(article);

        assertThatThrownBy(() -> articleService.getDraftDetail(1L, 1L))
                .isInstanceOf(BusinessException.class)
                .hasMessageContaining("文章版本不存在");
    }

    // ==================== listVersions ====================

    @Test
    @DisplayName("listVersions_文章不存在_抛出BusinessException")
    void listVersions_articleNotExists_throwsBusinessException() {
        when(articleDAO.getByIdAndNotDeleted(1L)).thenReturn(null);

        assertThatThrownBy(() -> articleService.listVersions(1L, 1L))
                .isInstanceOf(BusinessException.class)
                .hasMessageContaining("文章不存在");
    }

    @Test
    @DisplayName("listVersions_非作者操作_抛出BusinessException")
    void listVersions_notOwner_throwsBusinessException() {
        ArticleDO article = buildArticle(1L, 999L);
        when(articleDAO.getByIdAndNotDeleted(1L)).thenReturn(article);

        assertThatThrownBy(() -> articleService.listVersions(1L, 1L))
                .isInstanceOf(BusinessException.class)
                .hasMessageContaining("无权操作该文章");
    }

    @Test
    @DisplayName("listVersions_正常返回_调用converter转换版本列表")
    void listVersions_withValidOwner_convertsAndReturnsList() {
        ArticleDO article = buildArticle(1L, 1L);
        List<ArticleVersionDO> versions = List.of(buildVersion(200L, 1L), buildVersion(201L, 1L));
        List<ArticleVersionVO> expectedVOs = List.of(new ArticleVersionVO(), new ArticleVersionVO());

        when(articleDAO.getByIdAndNotDeleted(1L)).thenReturn(article);
        when(articleVersionDAO.listByArticleId(1L)).thenReturn(versions);
        when(articleConverter.toVersionVOList(versions)).thenReturn(expectedVOs);

        List<ArticleVersionVO> result = articleService.listVersions(1L, 1L);

        assertThat(result).hasSize(2);
        verify(articleConverter).toVersionVOList(versions);
    }

    // ==================== rollbackVersion ====================

    @Test
    @DisplayName("rollbackVersion_文章不存在_抛出BusinessException")
    void rollbackVersion_articleNotExists_throwsBusinessException() {
        when(articleDAO.getByIdAndNotDeleted(1L)).thenReturn(null);

        assertThatThrownBy(() -> articleService.rollbackVersion(1L, 1L, 200L))
                .isInstanceOf(BusinessException.class)
                .hasMessageContaining("文章不存在");
    }

    @Test
    @DisplayName("rollbackVersion_非作者操作_抛出BusinessException")
    void rollbackVersion_notOwner_throwsBusinessException() {
        ArticleDO article = buildArticle(1L, 999L);
        when(articleDAO.getByIdAndNotDeleted(1L)).thenReturn(article);

        assertThatThrownBy(() -> articleService.rollbackVersion(1L, 1L, 200L))
                .isInstanceOf(BusinessException.class)
                .hasMessageContaining("无权操作该文章");
    }

    @Test
    @DisplayName("rollbackVersion_versionId不存在_抛出BusinessException")
    void rollbackVersion_versionNotExists_throwsBusinessException() {
        ArticleDO article = buildArticle(1L, 1L);
        when(articleDAO.getByIdAndNotDeleted(1L)).thenReturn(article);
        when(articleVersionDAO.getVersionById(200L)).thenReturn(null);

        assertThatThrownBy(() -> articleService.rollbackVersion(1L, 1L, 200L))
                .isInstanceOf(BusinessException.class)
                .hasMessageContaining("文章版本不存在");
    }

    @Test
    @DisplayName("rollbackVersion_versionId属于其他文章_抛出BusinessException")
    void rollbackVersion_versionBelongsToOtherArticle_throwsBusinessException() {
        ArticleDO article = buildArticle(1L, 1L);
        ArticleVersionDO version = buildVersion(200L, 99L); // 属于文章 99，不是文章 1

        when(articleDAO.getByIdAndNotDeleted(1L)).thenReturn(article);
        when(articleVersionDAO.getVersionById(200L)).thenReturn(version);

        assertThatThrownBy(() -> articleService.rollbackVersion(1L, 1L, 200L))
                .isInstanceOf(BusinessException.class)
                .hasMessageContaining("文章版本不存在");
    }

    // ==================== getArticleDTO ====================

    @Test
    @DisplayName("getArticleDTO_文章不存在_抛出BusinessException")
    void getArticleDTO_articleNotExists_throwsBusinessException() {
        when(articleDAO.getByIdAndNotDeleted(1L)).thenReturn(null);

        assertThatThrownBy(() -> articleService.getArticleDTO(1L))
                .isInstanceOf(BusinessException.class)
                .hasMessageContaining("文章不存在");
    }

    @Test
    @DisplayName("getArticleDTO_文章未发布_返回null")
    void getArticleDTO_articleNotPublished_returnsNull() {
        ArticleDO article = buildArticle(1L, 1L);
        article.setPublishedVersionId(null);
        when(articleDAO.getByIdAndNotDeleted(1L)).thenReturn(article);

        ArticleDTO result = articleService.getArticleDTO(1L);

        assertThat(result).isNull();
    }

    @Test
    @DisplayName("getArticleDTO_正常发布文章_返回ArticleDTO")
    void getArticleDTO_withPublishedArticle_returnsArticleDTO() {
        ArticleDO article = buildPublishedArticle(1L, 1L, 200L);
        ArticleVersionDO version = buildVersion(200L, 1L);
        ArticleDTO expectedDTO = new ArticleDTO();
        ArticleStatisticsDO stats = new ArticleStatisticsDO();
        stats.setReadCount(100);
        stats.setCommentCount(5);

        when(articleDAO.getByIdAndNotDeleted(1L)).thenReturn(article);
        when(articleVersionDAO.getVersionById(200L)).thenReturn(version);
        when(articleConverter.toDTO(article, version)).thenReturn(expectedDTO);
        when(articleStatisticsDAO.getByArticleId(1L)).thenReturn(stats);

        ArticleDTO result = articleService.getArticleDTO(1L);

        assertThat(result).isNotNull();
        assertThat(result.getReadCount()).isEqualTo(100);
        assertThat(result.getCommentCount()).isEqualTo(5);
    }

    // ==================== getArticleDTOBatch ====================

    @Test
    @DisplayName("getArticleDTOBatch_空列表_返回空列表")
    void getArticleDTOBatch_emptyList_returnsEmptyList() {
        List<ArticleDTO> result = articleService.getArticleDTOBatch(List.of());

        assertThat(result).isEmpty();
    }

    @Test
    @DisplayName("getArticleDTOBatch_null输入_返回空列表")
    void getArticleDTOBatch_nullInput_returnsEmptyList() {
        List<ArticleDTO> result = articleService.getArticleDTOBatch(null);

        assertThat(result).isEmpty();
    }

    // ==================== 辅助方法 ====================

    private ArticleDO buildArticle(Long id, Long userId) {
        ArticleDO a = new ArticleDO();
        a.setId(id);
        a.setUserId(userId);
        a.setDeleted(DeleteStatusEnum.NOT_DELETED);
        return a;
    }

    private ArticleDO buildPublishedArticle(Long id, Long userId, Long publishedVersionId) {
        ArticleDO a = buildArticle(id, userId);
        a.setPublishedVersionId(publishedVersionId);
        a.setLatestVersionId(publishedVersionId);
        return a;
    }

    private ArticleVersionDO buildVersion(Long id, Long articleId) {
        ArticleVersionDO v = new ArticleVersionDO();
        v.setId(id);
        v.setArticleId(articleId);
        v.setTitle("测试标题");
        v.setContent("测试内容");
        return v;
    }

    private ArticleSaveRequest buildSaveRequest() {
        ArticleSaveRequest req = new ArticleSaveRequest();
        req.setTitle("测试文章");
        req.setContent("测试内容");
        return req;
    }

    /**
     * stub articleDAO.lambdaUpdate() 链式调用，避免 NPE。
     * ArticleDAO extends ServiceImpl，lambdaUpdate() 内部依赖 getBaseMapper()，
     * Mock 环境中 getBaseMapper() 返回 null 导致 NPE，需要 lenient stub 整个链路。
     */
    private void stubArticleLambdaUpdate() {
        com.baomidou.mybatisplus.extension.conditions.update.LambdaUpdateChainWrapper<ArticleDO> wrapper =
                org.mockito.Mockito.mock(
                        com.baomidou.mybatisplus.extension.conditions.update.LambdaUpdateChainWrapper.class);
        lenient().when(articleDAO.lambdaUpdate()).thenReturn(wrapper);
        lenient().when(wrapper.eq(any(), any())).thenReturn(wrapper);
        lenient().when(wrapper.set(any(), any())).thenReturn(wrapper);
        lenient().when(wrapper.set(any(boolean.class), any(), any())).thenReturn(wrapper);
        lenient().when(wrapper.setSql(any())).thenReturn(wrapper);
        lenient().when(wrapper.update()).thenReturn(true);
    }
}
