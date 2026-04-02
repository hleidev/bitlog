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
import top.harrylei.bitlog.article.repository.dao.TagDAO;
import top.harrylei.bitlog.article.repository.entity.TagDO;
import top.harrylei.bitlog.article.service.impl.TagServiceImpl;
import top.harrylei.bitlog.common.enums.DeleteStatusEnum;
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
    private ArticleConverter articleConverter;

    @InjectMocks
    private TagServiceImpl tagService;

    // ==================== listAll ====================

    @Test
    @DisplayName("listAll_正常查询_返回标签列表")
    void listAll_normal_returnsTagVOList() {
        List<TagDO> dos = List.of(buildTag(1L, "Java", false));
        List<TagVO> vos = List.of(new TagVO());
        when(tagDAO.listAllOrderByArticleCount()).thenReturn(dos);
        when(articleConverter.toTagVOList(dos)).thenReturn(vos);

        List<TagVO> result = tagService.listAll();

        assertThat(result).hasSize(1);
        verify(articleConverter).toTagVOList(dos);
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
    @DisplayName("getOrCreate_标签已存在且未删除_直接返回已有ID")
    void getOrCreate_tagExistsAndActive_returnsExistingId() {
        when(tagDAO.getByName("Java")).thenReturn(buildTag(1L, "Java", false));

        Long id = tagService.getOrCreate("Java");

        assertThat(id).isEqualTo(1L);
        verify(tagDAO, never()).save(any());
    }

    @Test
    @DisplayName("getOrCreate_标签已存在但已删除_恢复并返回ID")
    void getOrCreate_tagExistsAndDeleted_restoresAndReturnsId() {
        when(tagDAO.getByName("Java")).thenReturn(buildTag(1L, "Java", true));

        Long id = tagService.getOrCreate("Java");

        assertThat(id).isEqualTo(1L);
        verify(tagDAO).restore(1L);
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
        when(tagDAO.getByName("Java")).thenReturn(buildTag(1L, "Java", false));

        Long id = tagService.getOrCreate("  Java  ");

        assertThat(id).isEqualTo(1L);
        verify(tagDAO).getByName("Java");
    }

    // ==================== create ====================

    @Test
    @DisplayName("create_标签已存在且未删除_抛出BusinessException")
    void save_tagExistsAndActive_throwsBusinessException() {
        when(tagDAO.getByName("Java")).thenReturn(buildTag(1L, "Java", false));

        assertThatThrownBy(() -> tagService.save(buildSaveRequest("Java")))
                .isInstanceOf(BusinessException.class)
                .hasMessageContaining("标签已存在");
    }

    @Test
    @DisplayName("create_标签已存在但已删除_恢复并返回ID")
    void save_tagExistsAndDeleted_restoresAndReturnsId() {
        when(tagDAO.getByName("Java")).thenReturn(buildTag(1L, "Java", true));

        Long id = tagService.save(buildSaveRequest("Java"));

        assertThat(id).isEqualTo(1L);
        verify(tagDAO).restore(1L);
        verify(tagDAO, never()).save(any());
    }

    @Test
    @DisplayName("create_标签不存在_新建并返回ID")
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
    @DisplayName("update_标签已删除_抛出BusinessException")
    void update_tagDeleted_throwsBusinessException() {
        when(tagDAO.getById(1L)).thenReturn(buildTag(1L, "Java", true));

        assertThatThrownBy(() -> tagService.update(1L, buildUpdateRequest("新名称")))
                .isInstanceOf(BusinessException.class)
                .hasMessageContaining("标签不存在");
    }

    @Test
    @DisplayName("update_新名称与旧名称相同_跳过更新")
    void update_nameUnchanged_skipsUpdate() {
        when(tagDAO.getById(1L)).thenReturn(buildTag(1L, "Java", false));

        tagService.update(1L, buildUpdateRequest("Java"));

        verify(tagDAO, never()).updateById(any());
        verify(tagDAO, never()).getByName(any());
    }

    @Test
    @DisplayName("update_新名称被其他未删除标签占用_抛出BusinessException")
    void update_newNameConflictsWithActiveTag_throwsBusinessException() {
        when(tagDAO.getById(1L)).thenReturn(buildTag(1L, "Java", false));
        when(tagDAO.getByName("Python")).thenReturn(buildTag(2L, "Python", false));

        assertThatThrownBy(() -> tagService.update(1L, buildUpdateRequest("Python")))
                .isInstanceOf(BusinessException.class)
                .hasMessageContaining("标签已存在");
    }

    @Test
    @DisplayName("update_新名称与已删除标签同名_允许更新")
    void update_newNameConflictsWithDeletedTag_updatesSuccessfully() {
        when(tagDAO.getById(1L)).thenReturn(buildTag(1L, "Java", false));
        when(tagDAO.getByName("Python")).thenReturn(buildTag(2L, "Python", true));
        when(tagDAO.updateById(any())).thenReturn(true);

        tagService.update(1L, buildUpdateRequest("Python"));

        verify(tagDAO).updateById(any(TagDO.class));
    }

    @Test
    @DisplayName("update_新名称无冲突_正常更新")
    void update_newNameNoConflict_updatesSuccessfully() {
        when(tagDAO.getById(1L)).thenReturn(buildTag(1L, "Java", false));
        when(tagDAO.getByName("Go")).thenReturn(null);
        when(tagDAO.updateById(any())).thenReturn(true);

        tagService.update(1L, buildUpdateRequest("Go"));

        verify(tagDAO).updateById(any(TagDO.class));
    }

    // ==================== delete ====================

    @Test
    @DisplayName("delete_标签不存在_抛出BusinessException")
    void delete_tagNotExists_throwsBusinessException() {
        when(tagDAO.getById(1L)).thenReturn(null);

        assertThatThrownBy(() -> tagService.delete(1L))
                .isInstanceOf(BusinessException.class)
                .hasMessageContaining("标签不存在");
    }

    @Test
    @DisplayName("delete_标签已删除_抛出BusinessException")
    void delete_tagAlreadyDeleted_throwsBusinessException() {
        when(tagDAO.getById(1L)).thenReturn(buildTag(1L, "Java", true));

        assertThatThrownBy(() -> tagService.delete(1L))
                .isInstanceOf(BusinessException.class)
                .hasMessageContaining("标签不存在");
    }

    @Test
    @DisplayName("delete_标签存在且未删除_正常软删除")
    void delete_tagExistsAndActive_softDeletes() {
        when(tagDAO.getById(1L)).thenReturn(buildTag(1L, "Java", false));

        tagService.delete(1L);

        verify(tagDAO).delete(1L);
    }

    // ==================== 辅助方法 ====================

    private TagDO buildTag(Long id, String name, boolean deleted) {
        TagDO tag = new TagDO();
        tag.setId(id);
        tag.setName(name);
        tag.setArticleCount(0);
        tag.setDeleted(deleted ? DeleteStatusEnum.DELETED : DeleteStatusEnum.NOT_DELETED);
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
