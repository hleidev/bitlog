package top.harrylei.bitlog.article.converter;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import top.harrylei.bitlog.api.model.article.dto.ArticleDTO;
import top.harrylei.bitlog.api.model.article.vo.ArticleDetailVO;
import top.harrylei.bitlog.api.model.article.vo.ArticleVersionVO;
import top.harrylei.bitlog.api.model.article.vo.ArticleVO;
import top.harrylei.bitlog.article.repository.entity.ArticleDO;
import top.harrylei.bitlog.article.repository.entity.ArticleVersionDO;

import java.time.LocalDateTime;
import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;

@DisplayName("ArticleConverter 文章对象转换器测试")
class ArticleConverterTest {

    private ArticleConverter articleConverter;

    @BeforeEach
    void setUp() {
        articleConverter = new ArticleConverterImpl();
    }

    // ===== toVO =====

    @Test
    @DisplayName("toVO_articleId映射为VO的id字段")
    void toVO_articleIdMapsToVoId() {
        ArticleDO article = buildArticleDO(100L, 1L);
        ArticleVersionDO version = buildVersionDO(10L, 1L, "标题", "正文内容");

        ArticleVO vo = articleConverter.toVO(article, version);

        assertThat(vo).isNotNull();
        assertThat(vo.getId()).isEqualTo(1L);
    }

    @Test
    @DisplayName("toVO_content字段不出现在ArticleVO中")
    void toVO_contentIsNotMappedToVO() {
        ArticleDO article = buildArticleDO(100L, 1L);
        ArticleVersionDO version = buildVersionDO(10L, 1L, "标题", "这是正文内容，不应出现在VO里");

        ArticleVO vo = articleConverter.toVO(article, version);

        assertThat(vo).isNotNull();
        assertThat(vo).isInstanceOf(ArticleVO.class);
        assertThat(vo).isNotInstanceOf(ArticleDetailVO.class);
    }

    @Test
    @DisplayName("toVO_基础字段正确映射")
    void toVO_basicFieldsMappedCorrectly() {
        LocalDateTime createTime = LocalDateTime.of(2024, 1, 1, 12, 0);
        ArticleDO article = buildArticleDO(100L, 2L);
        article.setCreateTime(createTime);
        article.setCover("https://cdn/cover.jpg");
        article.setSummary("Java基础教程");
        article.setCategoryId(5L);
        ArticleVersionDO version = buildVersionDO(10L, 2L, "Java入门", "正文");

        ArticleVO vo = articleConverter.toVO(article, version);

        assertThat(vo.getId()).isEqualTo(2L);
        assertThat(vo.getUserId()).isEqualTo(100L);
        assertThat(vo.getTitle()).isEqualTo("Java入门");
        assertThat(vo.getSummary()).isEqualTo("Java基础教程");
        assertThat(vo.getCover()).isEqualTo("https://cdn/cover.jpg");
        assertThat(vo.getCategoryId()).isEqualTo(5L);
        assertThat(vo.getCreateTime()).isEqualTo(createTime);
    }

    @Test
    @DisplayName("toVO_忽略字段_readCount/commentCount/categoryName/topping/publishTime/updateTime为null")
    void toVO_ignoredFieldsAreNull() {
        ArticleDO article = buildArticleDO(100L, 1L);
        ArticleVersionDO version = buildVersionDO(1L, 1L, "标题", "正文");

        ArticleVO vo = articleConverter.toVO(article, version);

        assertThat(vo.getReadCount()).isNull();
        assertThat(vo.getCommentCount()).isNull();
        assertThat(vo.getCategoryName()).isNull();
        assertThat(vo.getTopping()).isNull();
        assertThat(vo.getPublishTime()).isNull();
        assertThat(vo.getUpdateTime()).isNull();
    }

    @Test
    @DisplayName("toVO_null输入_返回null")
    void toVO_withNullInput_returnsNull() {
        assertThat(articleConverter.toVO(null, null)).isNull();
    }

    // ===== toDetailVO =====

    @Test
    @DisplayName("toDetailVO_articleId映射为id_version.id映射为versionId")
    void toDetailVO_articleIdMapsToId_versionIdMapsToVersionId() {
        ArticleDO article = buildArticleDO(100L, 5L);
        ArticleVersionDO version = buildVersionDO(99L, 5L, "详情标题", "详细正文内容");
        version.setVersion(3);

        ArticleDetailVO vo = articleConverter.toDetailVO(article, version);

        assertThat(vo).isNotNull();
        assertThat(vo.getId()).isEqualTo(5L);
        assertThat(vo.getVersionId()).isEqualTo(99L);
        assertThat(vo.getVersion()).isEqualTo(3);
    }

    @Test
    @DisplayName("toDetailVO_content字段正确映射")
    void toDetailVO_contentIsMapped() {
        String content = "这是完整的文章正文内容";
        ArticleDO article = buildArticleDO(100L, 1L);
        ArticleVersionDO version = buildVersionDO(1L, 1L, "标题", content);

        ArticleDetailVO vo = articleConverter.toDetailVO(article, version);

        assertThat(vo).isNotNull();
        assertThat(vo.getContent()).isEqualTo(content);
    }

    @Test
    @DisplayName("toDetailVO_忽略字段为null")
    void toDetailVO_ignoredFieldsAreNull() {
        ArticleDO article = buildArticleDO(100L, 1L);
        ArticleVersionDO version = buildVersionDO(1L, 1L, "标题", "正文");

        ArticleDetailVO vo = articleConverter.toDetailVO(article, version);

        assertThat(vo.getReadCount()).isNull();
        assertThat(vo.getCommentCount()).isNull();
        assertThat(vo.getCategoryName()).isNull();
        assertThat(vo.getTags()).isNull();
        assertThat(vo.getTopping()).isNull();
        assertThat(vo.getPublishTime()).isNull();
        assertThat(vo.getUpdateTime()).isNull();
    }

    @Test
    @DisplayName("toDetailVO_null输入_返回null")
    void toDetailVO_withNullInput_returnsNull() {
        assertThat(articleConverter.toDetailVO(null, null)).isNull();
    }

    // ===== toVersionVO =====

    @Test
    @DisplayName("toVersionVO_所有字段正确映射")
    void toVersionVO_allFieldsMappedCorrectly() {
        LocalDateTime createTime = LocalDateTime.of(2024, 6, 15, 10, 30);
        ArticleVersionDO version = buildVersionDO(50L, 3L, "版本标题", "正文");
        version.setVersion(2);
        version.setCreateTime(createTime);

        ArticleVersionVO vo = articleConverter.toVersionVO(version);

        assertThat(vo).isNotNull();
        assertThat(vo.getId()).isEqualTo(50L);
        assertThat(vo.getArticleId()).isEqualTo(3L);
        assertThat(vo.getVersion()).isEqualTo(2);
        assertThat(vo.getTitle()).isEqualTo("版本标题");
        assertThat(vo.getCreateTime()).isEqualTo(createTime);
    }

    @Test
    @DisplayName("toVersionVO_null输入_返回null")
    void toVersionVO_withNullInput_returnsNull() {
        assertThat(articleConverter.toVersionVO(null)).isNull();
    }

    // ===== toVersionVOList =====

    @Test
    @DisplayName("toVersionVOList_多个版本_返回对应大小的列表")
    void toVersionVOList_withMultipleVersions_returnsList() {
        List<ArticleVersionDO> versions = List.of(
                buildVersionDO(1L, 10L, "标题1", "正文1"),
                buildVersionDO(2L, 10L, "标题2", "正文2"),
                buildVersionDO(3L, 10L, "标题3", "正文3")
        );

        List<ArticleVersionVO> result = articleConverter.toVersionVOList(versions);

        assertThat(result).hasSize(3);
        assertThat(result.get(0).getId()).isEqualTo(1L);
        assertThat(result.get(2).getId()).isEqualTo(3L);
    }

    @Test
    @DisplayName("toVersionVOList_null输入_返回null")
    void toVersionVOList_withNullInput_returnsNull() {
        assertThat(articleConverter.toVersionVOList(null)).isNull();
    }

    @Test
    @DisplayName("toVersionVOList_空列表_返回空列表")
    void toVersionVOList_withEmptyList_returnsEmptyList() {
        assertThat(articleConverter.toVersionVOList(List.of())).isEmpty();
    }

    // ===== toDTO =====

    @Test
    @DisplayName("toDTO_articleId映射为dto的id字段")
    void toDTO_articleIdMapsToId() {
        ArticleDO article = buildArticleDO(100L, 7L);
        article.setCover("cover.jpg");
        article.setSummary("DTO摘要");
        article.setCategoryId(3L);
        ArticleVersionDO version = buildVersionDO(20L, 7L, "DTO测试标题", "正文");

        ArticleDTO dto = articleConverter.toDTO(article, version);

        assertThat(dto).isNotNull();
        assertThat(dto.getId()).isEqualTo(7L);
        assertThat(dto.getUserId()).isEqualTo(100L);
        assertThat(dto.getTitle()).isEqualTo("DTO测试标题");
        assertThat(dto.getSummary()).isEqualTo("DTO摘要");
        assertThat(dto.getCover()).isEqualTo("cover.jpg");
        assertThat(dto.getCategoryId()).isEqualTo(3L);
    }

    @Test
    @DisplayName("toDTO_忽略字段为null")
    void toDTO_ignoredFieldsAreNull() {
        ArticleDO article = buildArticleDO(100L, 1L);
        ArticleVersionDO version = buildVersionDO(1L, 1L, "标题", "正文");

        ArticleDTO dto = articleConverter.toDTO(article, version);

        assertThat(dto.getTopping()).isNull();
        assertThat(dto.getPublishTime()).isNull();
        assertThat(dto.getReadCount()).isNull();
        assertThat(dto.getCommentCount()).isNull();
    }

    @Test
    @DisplayName("toDTO_null输入_返回null")
    void toDTO_withNullInput_returnsNull() {
        assertThat(articleConverter.toDTO(null, null)).isNull();
    }

    // ===== 辅助方法 =====

    private ArticleDO buildArticleDO(Long userId, Long articleId) {
        ArticleDO article = new ArticleDO();
        article.setId(articleId);
        article.setUserId(userId);
        return article;
    }

    private ArticleVersionDO buildVersionDO(Long id, Long articleId, String title, String content) {
        ArticleVersionDO version = new ArticleVersionDO();
        version.setId(id);
        version.setArticleId(articleId);
        version.setTitle(title);
        version.setContent(content);
        return version;
    }
}
