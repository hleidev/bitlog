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
        List<CategoryDO> dos = List.of(buildCategory(1L, "后端"));
        List<CategoryVO> vos = List.of(new CategoryVO());
        when(categoryDAO.listAll(null)).thenReturn(dos);
        when(articleConverter.toCategoryVOList(dos)).thenReturn(vos);

        List<CategoryVO> result = categoryService.listAll(null);

        assertThat(result).hasSize(1);
        verify(articleConverter).toCategoryVOList(dos);
    }

    // ==================== save ====================

    @Test
    @DisplayName("save_名称已存在_抛出BusinessException")
    void save_nameAlreadyExists_throwsBusinessException() {
        when(categoryDAO.getByName("后端")).thenReturn(buildCategory(1L, "后端"));

        assertThatThrownBy(() -> categoryService.save(buildCreateRequest("后端")))
                .isInstanceOf(BusinessException.class)
                .hasMessageContaining("分类已存在");
    }

    @Test
    @DisplayName("save_名称不存在_创建成功并返回ID")
    void save_nameNotExists_createsAndReturnsId() {
        when(categoryDAO.getByName("后端")).thenReturn(null);
        doAnswer(invocation -> {
            CategoryDO arg = invocation.getArgument(0);
            arg.setId(10L);
            return true;
        }).when(categoryDAO).save(any(CategoryDO.class));

        Long id = categoryService.save(buildCreateRequest("后端"));

        assertThat(id).isEqualTo(10L);
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
        when(categoryDAO.getById(1L)).thenReturn(buildCategory(1L, "后端"));

        categoryService.update(1L, buildUpdateRequest("后端"));

        verify(categoryDAO, never()).getByName(any());
        verify(categoryDAO, never()).updateById(any());
    }

    @Test
    @DisplayName("update_新名称与其他分类冲突_抛出BusinessException")
    void update_newNameConflicts_throwsBusinessException() {
        when(categoryDAO.getById(1L)).thenReturn(buildCategory(1L, "后端"));
        when(categoryDAO.getByName("前端")).thenReturn(buildCategory(2L, "前端"));

        assertThatThrownBy(() -> categoryService.update(1L, buildUpdateRequest("前端")))
                .isInstanceOf(BusinessException.class)
                .hasMessageContaining("分类已存在");
    }

    @Test
    @DisplayName("update_新名称无冲突_正常更新")
    void update_newNameNoConflict_updatesSuccessfully() {
        when(categoryDAO.getById(1L)).thenReturn(buildCategory(1L, "后端"));
        when(categoryDAO.getByName("全栈")).thenReturn(null);
        when(categoryDAO.updateById(any())).thenReturn(true);

        categoryService.update(1L, buildUpdateRequest("全栈"));

        verify(categoryDAO).getByName("全栈");
        verify(categoryDAO).updateById(any(CategoryDO.class));
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
    @DisplayName("delete_存在关联文章_抛出BusinessException")
    void delete_hasArticles_throwsBusinessException() {
        CategoryDO category = buildCategory(1L, "后端");
        category.setArticleCount(3);
        when(categoryDAO.getById(1L)).thenReturn(category);

        assertThatThrownBy(() -> categoryService.delete(1L))
                .isInstanceOf(BusinessException.class)
                .hasMessageContaining("文章");
    }

    @Test
    @DisplayName("delete_无关联文章_删除成功")
    void delete_noArticles_deletesSuccessfully() {
        when(categoryDAO.getById(1L)).thenReturn(buildCategory(1L, "后端"));

        categoryService.delete(1L);

        verify(categoryDAO).removeById(1L);
    }

    // ==================== 辅助方法 ====================

    private CategoryDO buildCategory(Long id, String name) {
        CategoryDO c = new CategoryDO();
        c.setId(id);
        c.setName(name);
        c.setArticleCount(0);
        return c;
    }

    private CategoryCreateRequest buildCreateRequest(String name) {
        CategoryCreateRequest req = new CategoryCreateRequest();
        req.setName(name);
        return req;
    }

    private CategoryUpdateRequest buildUpdateRequest(String name) {
        CategoryUpdateRequest req = new CategoryUpdateRequest();
        req.setName(name);
        return req;
    }
}
