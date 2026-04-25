package top.harrylei.bitlog.article.service;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import top.harrylei.bitlog.api.model.article.req.CategoryCreateRequest;
import top.harrylei.bitlog.api.model.article.req.CategoryUpdateRequest;
import top.harrylei.bitlog.api.model.article.vo.CategoryVO;
import top.harrylei.bitlog.article.converter.ArticleConverter;
import top.harrylei.bitlog.article.repository.dao.CategoryDAO;
import top.harrylei.bitlog.article.repository.entity.CategoryDO;
import top.harrylei.bitlog.article.service.impl.CategoryServiceImpl;
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
@DisplayName("CategoryServiceImpl 分类服务测试")
class CategoryServiceImplTest {

    @Mock
    private CategoryDAO categoryDAO;

    @Mock
    private ArticleConverter articleConverter;

    @InjectMocks
    private CategoryServiceImpl categoryService;

    // ==================== listAll ====================

    @Test
    @DisplayName("listAll_无搜索词_返回全量分类列表")
    void listAll_noFilter_returnsCategoryVOList() {
        List<CategoryDO> dos = List.of(buildCategory(1L, "后端", 0L));
        List<CategoryVO> vos = List.of(new CategoryVO());
        when(categoryDAO.listAll(null)).thenReturn(dos);
        when(articleConverter.toCategoryVOList(dos)).thenReturn(vos);

        List<CategoryVO> result = categoryService.listAll(null);

        assertThat(result).hasSize(1);
        verify(articleConverter).toCategoryVOList(dos);
    }

    // ==================== save ====================

    @Test
    @DisplayName("save_父分类不存在_抛出BusinessException")
    void save_parentNotExists_throwsBusinessException() {
        when(categoryDAO.getById(99L)).thenReturn(null);

        assertThatThrownBy(() -> categoryService.save(buildCreateRequest("子分类", 99L)))
                .isInstanceOf(BusinessException.class)
                .hasMessageContaining("父分类不存在");
    }

    @Test
    @DisplayName("save_父分类本身是子分类_抛出BusinessException")
    void save_parentIsSubCategory_throwsBusinessException() {
        when(categoryDAO.getById(2L)).thenReturn(buildCategory(2L, "后端", 1L));

        assertThatThrownBy(() -> categoryService.save(buildCreateRequest("子子分类", 2L)))
                .isInstanceOf(BusinessException.class)
                .hasMessageContaining("父分类不存在或不是顶级分类");
    }

    @Test
    @DisplayName("save_同级名称已存在_抛出BusinessException")
    void save_sameNameSameLevel_throwsBusinessException() {
        when(categoryDAO.getByName("后端", 0L)).thenReturn(buildCategory(1L, "后端", 0L));

        assertThatThrownBy(() -> categoryService.save(buildCreateRequest("后端", null)))
                .isInstanceOf(BusinessException.class)
                .hasMessageContaining("分类已存在");
    }

    @Test
    @DisplayName("save_顶级分类不存在_创建成功并返回ID")
    void save_topLevelNotExists_createsAndReturnsId() {
        when(categoryDAO.getByName("后端", 0L)).thenReturn(null);
        doAnswer(invocation -> {
            CategoryDO arg = invocation.getArgument(0);
            arg.setId(10L);
            return true;
        }).when(categoryDAO).save(any(CategoryDO.class));

        Long id = categoryService.save(buildCreateRequest("后端", null));

        assertThat(id).isEqualTo(10L);
        verify(categoryDAO).save(any(CategoryDO.class));
    }

    @Test
    @DisplayName("save_子分类不存在_创建成功并返回ID")
    void save_subCategoryNotExists_createsAndReturnsId() {
        when(categoryDAO.getById(1L)).thenReturn(buildCategory(1L, "技术", 0L));
        when(categoryDAO.getByName("后端", 1L)).thenReturn(null);
        doAnswer(invocation -> {
            CategoryDO arg = invocation.getArgument(0);
            arg.setId(20L);
            return true;
        }).when(categoryDAO).save(any(CategoryDO.class));

        Long id = categoryService.save(buildCreateRequest("后端", 1L));

        assertThat(id).isEqualTo(20L);
        verify(categoryDAO).save(any(CategoryDO.class));
    }

    // ==================== update ====================

    @Test
    @DisplayName("update_分类不存在_抛出BusinessException")
    void update_categoryNotExists_throwsBusinessException() {
        when(categoryDAO.getById(1L)).thenReturn(null);

        assertThatThrownBy(() -> categoryService.update(1L, buildUpdateRequest("新名称")))
                .isInstanceOf(BusinessException.class)
                .hasMessageContaining("分类不存在");
    }

    @Test
    @DisplayName("update_名称未变_跳过DB写操作")
    void update_nameUnchanged_skipsDbWrite() {
        when(categoryDAO.getById(1L)).thenReturn(buildCategory(1L, "后端", 0L));

        categoryService.update(1L, buildUpdateRequest("后端"));

        verify(categoryDAO, never()).getByName(any(), any());
        verify(categoryDAO, never()).updateById(any());
    }

    @Test
    @DisplayName("update_新名称与同级其他分类冲突_抛出BusinessException")
    void update_newNameConflictsWithSibling_throwsBusinessException() {
        when(categoryDAO.getById(1L)).thenReturn(buildCategory(1L, "后端", 0L));
        when(categoryDAO.getByName("前端", 0L)).thenReturn(buildCategory(2L, "前端", 0L));

        assertThatThrownBy(() -> categoryService.update(1L, buildUpdateRequest("前端")))
                .isInstanceOf(BusinessException.class)
                .hasMessageContaining("分类已存在");
    }

    @Test
    @DisplayName("update_新名称无冲突_正常更新")
    void update_newNameNoConflict_updatesSuccessfully() {
        when(categoryDAO.getById(1L)).thenReturn(buildCategory(1L, "后端", 0L));
        when(categoryDAO.getByName("全栈", 0L)).thenReturn(null);
        when(categoryDAO.updateById(any())).thenReturn(true);

        categoryService.update(1L, buildUpdateRequest("全栈"));

        verify(categoryDAO).getByName("全栈", 0L);
        verify(categoryDAO).updateById(any(CategoryDO.class));
    }

    @Test
    @DisplayName("update_名称和归属均未变_跳过DB写操作")
    void update_nameAndParentUnchanged_skipsDbWrite() {
        when(categoryDAO.getById(1L)).thenReturn(buildCategory(1L, "后端", 0L));

        categoryService.update(1L, buildUpdateRequest("后端", null));

        verify(categoryDAO, never()).updateById(any());
    }

    @Test
    @DisplayName("update_顶级分类有子分类时修改归属_抛出BusinessException")
    void update_topLevelHasChildren_moveForbidden() {
        when(categoryDAO.getById(1L)).thenReturn(buildCategory(1L, "技术", 0L));
        when(categoryDAO.hasChildren(1L)).thenReturn(true);

        assertThatThrownBy(() -> categoryService.update(1L, buildUpdateRequest("技术", 2L)))
                .isInstanceOf(BusinessException.class)
                .hasMessageContaining("存在子分类");
    }

    @Test
    @DisplayName("update_目标父分类不存在_抛出BusinessException")
    void update_targetParentNotExists_throwsBusinessException() {
        when(categoryDAO.getById(1L)).thenReturn(buildCategory(1L, "后端", 0L));
        when(categoryDAO.getById(99L)).thenReturn(null);

        assertThatThrownBy(() -> categoryService.update(1L, buildUpdateRequest("后端", 99L)))
                .isInstanceOf(BusinessException.class)
                .hasMessageContaining("父分类不存在");
    }

    @Test
    @DisplayName("update_目标父分类本身是子分类_抛出BusinessException")
    void update_targetParentIsSubCategory_throwsBusinessException() {
        when(categoryDAO.getById(1L)).thenReturn(buildCategory(1L, "后端", 0L));
        when(categoryDAO.getById(2L)).thenReturn(buildCategory(2L, "Java", 1L));

        assertThatThrownBy(() -> categoryService.update(1L, buildUpdateRequest("后端", 2L)))
                .isInstanceOf(BusinessException.class)
                .hasMessageContaining("父分类不存在");
    }

    @Test
    @DisplayName("update_移动到顶级分类_更新成功")
    void update_moveToTopLevel_updatesSuccessfully() {
        when(categoryDAO.getById(1L)).thenReturn(buildCategory(1L, "后端", 2L));
        when(categoryDAO.getByName("后端", 0L)).thenReturn(null);
        when(categoryDAO.updateById(any())).thenReturn(true);

        categoryService.update(1L, buildUpdateRequest("后端", 0L));

        verify(categoryDAO).updateById(any(CategoryDO.class));
    }

    @Test
    @DisplayName("update_移动到其他父分类_更新成功")
    void update_moveToAnotherParent_updatesSuccessfully() {
        when(categoryDAO.getById(1L)).thenReturn(buildCategory(1L, "后端", 2L));
        when(categoryDAO.getById(3L)).thenReturn(buildCategory(3L, "工程", 0L));
        when(categoryDAO.getByName("后端", 3L)).thenReturn(null);
        when(categoryDAO.updateById(any())).thenReturn(true);

        categoryService.update(1L, buildUpdateRequest("后端", 3L));

        verify(categoryDAO).updateById(any(CategoryDO.class));
    }

    @Test
    @DisplayName("update_移动后目标层级名称冲突_抛出BusinessException")
    void update_moveWithNameConflictAtNewLevel_throwsBusinessException() {
        when(categoryDAO.getById(1L)).thenReturn(buildCategory(1L, "后端", 2L));
        when(categoryDAO.getById(3L)).thenReturn(buildCategory(3L, "工程", 0L));
        when(categoryDAO.getByName("后端", 3L)).thenReturn(buildCategory(5L, "后端", 3L));

        assertThatThrownBy(() -> categoryService.update(1L, buildUpdateRequest("后端", 3L)))
                .isInstanceOf(BusinessException.class)
                .hasMessageContaining("分类已存在");
    }

    // ==================== delete ====================

    @Test
    @DisplayName("delete_分类不存在_抛出BusinessException")
    void delete_categoryNotExists_throwsBusinessException() {
        when(categoryDAO.getById(1L)).thenReturn(null);

        assertThatThrownBy(() -> categoryService.delete(1L))
                .isInstanceOf(BusinessException.class)
                .hasMessageContaining("分类不存在");
    }

    @Test
    @DisplayName("delete_存在子分类_抛出BusinessException")
    void delete_hasChildren_throwsBusinessException() {
        when(categoryDAO.getById(1L)).thenReturn(buildCategory(1L, "技术", 0L));
        when(categoryDAO.hasChildren(1L)).thenReturn(true);

        assertThatThrownBy(() -> categoryService.delete(1L))
                .isInstanceOf(BusinessException.class)
                .hasMessageContaining("子分类");
    }

    @Test
    @DisplayName("delete_存在关联文章_抛出BusinessException")
    void delete_hasArticles_throwsBusinessException() {
        CategoryDO category = buildCategory(1L, "后端", 0L);
        category.setArticleCount(3);
        when(categoryDAO.getById(1L)).thenReturn(category);
        when(categoryDAO.hasChildren(1L)).thenReturn(false);

        assertThatThrownBy(() -> categoryService.delete(1L))
                .isInstanceOf(BusinessException.class)
                .hasMessageContaining("文章");
    }

    @Test
    @DisplayName("delete_无子分类无文章_删除成功")
    void delete_noChildrenNoArticles_deletesSuccessfully() {
        when(categoryDAO.getById(1L)).thenReturn(buildCategory(1L, "后端", 0L));
        when(categoryDAO.hasChildren(1L)).thenReturn(false);

        categoryService.delete(1L);

        verify(categoryDAO).removeById(1L);
    }

    // ==================== 辅助方法 ====================

    private CategoryDO buildCategory(Long id, String name, Long parentId) {
        CategoryDO c = new CategoryDO();
        c.setId(id);
        c.setParentId(parentId);
        c.setName(name);
        c.setArticleCount(0);
        return c;
    }

    private CategoryCreateRequest buildCreateRequest(String name, Long parentId) {
        CategoryCreateRequest req = new CategoryCreateRequest();
        req.setName(name);
        req.setParentId(parentId);
        return req;
    }

    private CategoryUpdateRequest buildUpdateRequest(String name) {
        return buildUpdateRequest(name, null);
    }

    private CategoryUpdateRequest buildUpdateRequest(String name, Long parentId) {
        CategoryUpdateRequest req = new CategoryUpdateRequest();
        req.setName(name);
        req.setParentId(parentId);
        return req;
    }
}
