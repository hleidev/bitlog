package top.harrylei.bitlog.article.service;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import top.harrylei.bitlog.api.model.article.req.CategorySaveRequest;
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
    @DisplayName("listAll_正常查询_返回分类列表")
    void listAll_normal_returnsCategoryVOList() {
        List<CategoryDO> dos = List.of(buildCategory(1L, "后端"));
        List<CategoryVO> vos = List.of(new CategoryVO());
        when(categoryDAO.listAllOrderByArticleCount()).thenReturn(dos);
        when(articleConverter.toCategoryVOList(dos)).thenReturn(vos);

        List<CategoryVO> result = categoryService.listAll();

        assertThat(result).hasSize(1);
        verify(articleConverter).toCategoryVOList(dos);
    }

    // ==================== getOrCreate ====================

    @Test
    @DisplayName("getOrCreate_name为null_抛出BusinessException")
    void getOrCreate_nameIsNull_throwsBusinessException() {
        assertThatThrownBy(() -> categoryService.getOrCreate(null))
                .isInstanceOf(BusinessException.class);
    }

    @Test
    @DisplayName("getOrCreate_name为空白_抛出BusinessException")
    void getOrCreate_nameIsBlank_throwsBusinessException() {
        assertThatThrownBy(() -> categoryService.getOrCreate("   "))
                .isInstanceOf(BusinessException.class);
    }

    @Test
    @DisplayName("getOrCreate_分类已存在_直接返回已有ID")
    void getOrCreate_categoryExists_returnsExistingId() {
        when(categoryDAO.getByName("后端")).thenReturn(buildCategory(1L, "后端"));

        Long id = categoryService.getOrCreate("后端");

        assertThat(id).isEqualTo(1L);
        verify(categoryDAO, never()).save(any());
    }

    @Test
    @DisplayName("getOrCreate_分类不存在_新建并返回ID")
    void getOrCreate_categoryNotExists_createsAndReturnsId() {
        when(categoryDAO.getByName("后端")).thenReturn(null);
        doAnswer(invocation -> {
            CategoryDO arg = invocation.getArgument(0);
            arg.setId(10L);
            return true;
        }).when(categoryDAO).save(any(CategoryDO.class));

        Long id = categoryService.getOrCreate("后端");

        assertThat(id).isEqualTo(10L);
        verify(categoryDAO).save(any(CategoryDO.class));
    }

    @Test
    @DisplayName("getOrCreate_name包含前后空格_trim后查询")
    void getOrCreate_nameWithSpaces_trimBeforeQuery() {
        when(categoryDAO.getByName("后端")).thenReturn(buildCategory(1L, "后端"));

        Long id = categoryService.getOrCreate("  后端  ");

        assertThat(id).isEqualTo(1L);
        verify(categoryDAO).getByName("后端");
    }

    // ==================== save ====================

    @Test
    @DisplayName("save_分类名已存在_抛出BusinessException")
    void save_categoryExists_throwsBusinessException() {
        when(categoryDAO.getByName("后端")).thenReturn(buildCategory(1L, "后端"));

        assertThatThrownBy(() -> categoryService.save(buildSaveRequest("后端", null, null)))
                .isInstanceOf(BusinessException.class)
                .hasMessageContaining("分类已存在");
    }

    @Test
    @DisplayName("save_分类不存在_新建并返回ID")
    void save_categoryNotExists_createsAndReturnsId() {
        when(categoryDAO.getByName("后端")).thenReturn(null);
        doAnswer(invocation -> {
            CategoryDO arg = invocation.getArgument(0);
            arg.setId(20L);
            return true;
        }).when(categoryDAO).save(any(CategoryDO.class));

        Long id = categoryService.save(buildSaveRequest("后端", "描述", 1));

        assertThat(id).isEqualTo(20L);
        verify(categoryDAO).save(any(CategoryDO.class));
    }

    // ==================== update ====================

    @Test
    @DisplayName("update_分类不存在_抛出BusinessException")
    void update_categoryNotExists_throwsBusinessException() {
        when(categoryDAO.getById(1L)).thenReturn(null);

        assertThatThrownBy(() -> categoryService.update(1L, buildSaveRequest("新名称", null, null)))
                .isInstanceOf(BusinessException.class)
                .hasMessageContaining("分类不存在");
    }

    @Test
    @DisplayName("update_新名称与其他分类冲突_抛出BusinessException")
    void update_newNameConflicts_throwsBusinessException() {
        when(categoryDAO.getById(1L)).thenReturn(buildCategory(1L, "后端"));
        when(categoryDAO.getByName("前端")).thenReturn(buildCategory(2L, "前端"));

        assertThatThrownBy(() -> categoryService.update(1L, buildSaveRequest("前端", null, null)))
                .isInstanceOf(BusinessException.class)
                .hasMessageContaining("分类已存在");
    }

    @Test
    @DisplayName("update_名称未变_跳过冲突检查直接更新")
    void update_nameUnchanged_skipsConflictCheck() {
        when(categoryDAO.getById(1L)).thenReturn(buildCategory(1L, "后端"));
        when(categoryDAO.updateById(any())).thenReturn(true);

        categoryService.update(1L, buildSaveRequest("后端", "新描述", null));

        verify(categoryDAO, never()).getByName(any());
        verify(categoryDAO).updateById(any(CategoryDO.class));
    }

    @Test
    @DisplayName("update_新名称无冲突_正常更新")
    void update_newNameNoConflict_updatesSuccessfully() {
        when(categoryDAO.getById(1L)).thenReturn(buildCategory(1L, "后端"));
        when(categoryDAO.getByName("全栈")).thenReturn(null);
        when(categoryDAO.updateById(any())).thenReturn(true);

        categoryService.update(1L, buildSaveRequest("全栈", null, null));

        verify(categoryDAO).getByName("全栈");
        verify(categoryDAO).updateById(any(CategoryDO.class));
    }

    // ==================== delete ====================

    @Test
    @DisplayName("delete_分类不存在_抛出BusinessException")
    void delete_categoryNotExists_throwsBusinessException() {
        when(categoryDAO.removeById(1L)).thenReturn(false);

        assertThatThrownBy(() -> categoryService.delete(1L))
                .isInstanceOf(BusinessException.class)
                .hasMessageContaining("分类不存在");
    }

    @Test
    @DisplayName("delete_分类存在_物理删除成功")
    void delete_categoryExists_deletesSuccessfully() {
        when(categoryDAO.removeById(1L)).thenReturn(true);

        categoryService.delete(1L);

        verify(categoryDAO).removeById(1L);
    }

    // ==================== 辅助方法 ====================

    private CategoryDO buildCategory(Long id, String name) {
        CategoryDO c = new CategoryDO();
        c.setId(id);
        c.setName(name);
        c.setDescription("");
        c.setSortOrder(0);
        c.setArticleCount(0);
        return c;
    }

    private CategorySaveRequest buildSaveRequest(String name, String description, Integer sortOrder) {
        CategorySaveRequest req = new CategorySaveRequest();
        req.setName(name);
        req.setDescription(description);
        req.setSortOrder(sortOrder);
        return req;
    }
}
