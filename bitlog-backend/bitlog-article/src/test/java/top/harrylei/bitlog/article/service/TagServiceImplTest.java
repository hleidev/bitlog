package top.harrylei.bitlog.article.service;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import top.harrylei.bitlog.api.model.article.req.TagSaveRequest;
import top.harrylei.bitlog.api.model.article.req.TagUpdateRequest;
import top.harrylei.bitlog.api.model.article.vo.TagVO;
import top.harrylei.bitlog.article.converter.ArticleConverter;
import top.harrylei.bitlog.article.repository.dao.ArticleTagDAO;
import top.harrylei.bitlog.article.repository.dao.TagDAO;
import top.harrylei.bitlog.article.repository.entity.TagDO;
import top.harrylei.bitlog.article.service.impl.TagServiceImpl;
import top.harrylei.bitlog.common.exception.BusinessException;

import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.doAnswer;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
@DisplayName("TagServiceImpl 标签服务测试")
class TagServiceImplTest {

    @Mock
    private TagDAO tagDAO;

    @Mock
    private ArticleTagDAO articleTagDAO;

    @Mock
    private ArticleConverter articleConverter;

    @InjectMocks
    private TagServiceImpl tagService;

    // ==================== listAll ====================

    @Test
    @DisplayName("listAll_无搜索词_返回全量标签列表")
    void listAll_noName_returnsAllTags() {
        List<TagDO> dos = List.of(buildTag(1L, "Java"));
        List<TagVO> vos = List.of(new TagVO());
        when(tagDAO.listAll(null)).thenReturn(dos);
        when(articleConverter.toTagVOList(dos)).thenReturn(vos);

        List<TagVO> result = tagService.listAll(null);

        assertThat(result).hasSize(1);
        verify(articleConverter).toTagVOList(dos);
    }

    @Test
    @DisplayName("listAll_带搜索词_按名称模糊过滤")
    void listAll_withName_filtersResults() {
        List<TagDO> dos = List.of(buildTag(1L, "Java"));
        List<TagVO> vos = List.of(new TagVO());
        when(tagDAO.listAll("Java")).thenReturn(dos);
        when(articleConverter.toTagVOList(dos)).thenReturn(vos);

        List<TagVO> result = tagService.listAll("Java");

        assertThat(result).hasSize(1);
        verify(tagDAO).listAll("Java");
    }

    // ==================== getOrCreate ====================

    @Test
    @DisplayName("getOrCreate_name为null_抛出BusinessException")
    void getOrCreate_nameIsNull_throwsBusinessException() {
        assertThatThrownBy(() -> tagService.getOrCreate(null))
                .isInstanceOf(BusinessException.class);
    }

    @Test
    @DisplayName("getOrCreate_name为空白_抛出BusinessException")
    void getOrCreate_nameIsBlank_throwsBusinessException() {
        assertThatThrownBy(() -> tagService.getOrCreate("   "))
                .isInstanceOf(BusinessException.class);
    }

    @Test
    @DisplayName("getOrCreate_标签已存在_直接返回已有ID")
    void getOrCreate_tagExists_returnsExistingId() {
        when(tagDAO.getByName("Java")).thenReturn(buildTag(1L, "Java"));

        Long id = tagService.getOrCreate("Java");

        assertThat(id).isEqualTo(1L);
        verify(tagDAO, never()).save(any());
    }

    @Test
    @DisplayName("getOrCreate_标签不存在_新建并返回ID")
    void getOrCreate_tagNotExists_createsAndReturnsId() {
        when(tagDAO.getByName("Java")).thenReturn(null);
        doAnswer(invocation -> {
            TagDO arg = invocation.getArgument(0);
            arg.setId(10L);
            return true;
        }).when(tagDAO).save(any(TagDO.class));

        Long id = tagService.getOrCreate("Java");

        assertThat(id).isEqualTo(10L);
        verify(tagDAO).save(any(TagDO.class));
    }

    @Test
    @DisplayName("getOrCreate_name包含前后空格_trim后查询")
    void getOrCreate_nameWithSpaces_trimBeforeQuery() {
        when(tagDAO.getByName("Java")).thenReturn(buildTag(1L, "Java"));

        Long id = tagService.getOrCreate("  Java  ");

        assertThat(id).isEqualTo(1L);
        verify(tagDAO).getByName("Java");
    }

    // ==================== save ====================

    @Test
    @DisplayName("save_标签名已存在_抛出BusinessException")
    void save_tagExists_throwsBusinessException() {
        when(tagDAO.getByName("Java")).thenReturn(buildTag(1L, "Java"));

        assertThatThrownBy(() -> tagService.save(buildSaveRequest("Java")))
                .isInstanceOf(BusinessException.class)
                .hasMessageContaining("标签已存在");
    }

    @Test
    @DisplayName("save_标签不存在_新建并返回ID")
    void save_tagNotExists_createsAndReturnsId() {
        when(tagDAO.getByName("Java")).thenReturn(null);
        doAnswer(invocation -> {
            TagDO arg = invocation.getArgument(0);
            arg.setId(20L);
            return true;
        }).when(tagDAO).save(any(TagDO.class));

        Long id = tagService.save(buildSaveRequest("Java"));

        assertThat(id).isEqualTo(20L);
        verify(tagDAO).save(any(TagDO.class));
    }

    // ==================== update ====================

    @Test
    @DisplayName("update_标签不存在_抛出BusinessException")
    void update_tagNotExists_throwsBusinessException() {
        when(tagDAO.getById(1L)).thenReturn(null);

        assertThatThrownBy(() -> tagService.update(1L, buildUpdateRequest("新名称")))
                .isInstanceOf(BusinessException.class)
                .hasMessageContaining("标签不存在");
    }

    @Test
    @DisplayName("update_新名称与旧名称相同_跳过更新")
    void update_nameUnchanged_skipsUpdate() {
        when(tagDAO.getById(1L)).thenReturn(buildTag(1L, "Java"));

        tagService.update(1L, buildUpdateRequest("Java"));

        verify(tagDAO, never()).updateById(any());
        verify(tagDAO, never()).getByName(any());
    }

    @Test
    @DisplayName("update_新名称被其他标签占用_抛出BusinessException")
    void update_newNameConflicts_throwsBusinessException() {
        when(tagDAO.getById(1L)).thenReturn(buildTag(1L, "Java"));
        when(tagDAO.getByName("Python")).thenReturn(buildTag(2L, "Python"));

        assertThatThrownBy(() -> tagService.update(1L, buildUpdateRequest("Python")))
                .isInstanceOf(BusinessException.class)
                .hasMessageContaining("标签已存在");
    }

    @Test
    @DisplayName("update_新名称无冲突_正常更新")
    void update_newNameNoConflict_updatesSuccessfully() {
        when(tagDAO.getById(1L)).thenReturn(buildTag(1L, "Java"));
        when(tagDAO.getByName("Go")).thenReturn(null);
        when(tagDAO.updateById(any())).thenReturn(true);

        tagService.update(1L, buildUpdateRequest("Go"));

        verify(tagDAO).updateById(any(TagDO.class));
    }

    // ==================== batchDelete ====================

    @Test
    @DisplayName("batchDelete_正常删除_物理删除标签并清理关联")
    void batchDelete_normal_deletesAndClearsAssociations() {
        List<Long> ids = List.of(1L, 2L);

        tagService.batchDelete(ids);

        verify(tagDAO).removeByIds(ids);
        verify(articleTagDAO).removeByTagIds(ids);
    }

    @Test
    @DisplayName("batchDelete_单个ID_正常删除")
    void batchDelete_singleId_deletesSuccessfully() {
        List<Long> ids = List.of(1L);

        tagService.batchDelete(ids);

        verify(tagDAO).removeByIds(ids);
        verify(articleTagDAO).removeByTagIds(ids);
    }

    // ==================== 辅助方法 ====================

    private TagDO buildTag(Long id, String name) {
        TagDO tag = new TagDO();
        tag.setId(id);
        tag.setName(name);
        tag.setArticleCount(0);
        return tag;
    }

    private TagSaveRequest buildSaveRequest(String name) {
        TagSaveRequest req = new TagSaveRequest();
        req.setName(name);
        return req;
    }

    private TagUpdateRequest buildUpdateRequest(String name) {
        TagUpdateRequest req = new TagUpdateRequest();
        req.setName(name);
        return req;
    }
}
