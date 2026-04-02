package top.harrylei.bitlog.article.controller;

import com.fasterxml.jackson.databind.ObjectMapper;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.setup.MockMvcBuilders;
import top.harrylei.bitlog.api.model.article.req.CategorySaveRequest;
import top.harrylei.bitlog.api.model.article.vo.CategoryVO;
import top.harrylei.bitlog.article.service.CategoryService;
import top.harrylei.bitlog.common.advice.GlobalExceptionHandler;

import java.util.List;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.doNothing;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.delete;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.put;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@ExtendWith(MockitoExtension.class)
@DisplayName("CategoryController 分类接口测试")
class CategoryControllerTest {

    @Mock
    private CategoryService categoryService;

    @InjectMocks
    private CategoryController categoryController;

    private MockMvc mockMvc;
    private final ObjectMapper objectMapper = new ObjectMapper();

    @BeforeEach
    void setUp() {
        mockMvc = MockMvcBuilders
                .standaloneSetup(categoryController)
                .setControllerAdvice(new GlobalExceptionHandler())
                .build();
    }

    // ==================== GET /api/v1/category ====================

    @Test
    @DisplayName("listAll_正常查询_返回分类列表")
    void listAll_normal_returnsCategoryList() throws Exception {
        CategoryVO vo = new CategoryVO().setId(1L).setName("后端").setArticleCount(5);
        when(categoryService.listAll()).thenReturn(List.of(vo));

        mockMvc.perform(get("/api/v1/category"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.code").value(0))
                .andExpect(jsonPath("$.data[0].id").value(1))
                .andExpect(jsonPath("$.data[0].name").value("后端"));
    }

    // ==================== POST /api/v1/category/get-or-create ====================

    @Test
    @DisplayName("getOrCreate_正常请求_返回分类ID")
    void getOrCreate_normal_returnsId() throws Exception {
        when(categoryService.getOrCreate("后端")).thenReturn(1L);

        mockMvc.perform(post("/api/v1/category/get-or-create")
                        .param("name", "后端"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.code").value(0))
                .andExpect(jsonPath("$.data").value(1));
    }

    // ==================== POST /api/v1/category ====================

    @Test
    @DisplayName("create_合法请求体_返回新分类ID")
    void save_validRequest_returnsId() throws Exception {
        when(categoryService.save(any())).thenReturn(2L);

        mockMvc.perform(post("/api/v1/category")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(toJson(buildSaveRequest("前端", "前端技术", 1))))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.code").value(0))
                .andExpect(jsonPath("$.data").value(2));
    }

    @Test
    @DisplayName("create_分类名为空_返回参数校验失败")
    void save_blankName_returnsValidationError() throws Exception {
        mockMvc.perform(post("/api/v1/category")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(toJson(buildSaveRequest("", null, null))))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.code").value(40000))
                .andExpect(jsonPath("$.data").isEmpty());
    }

    // ==================== PUT /api/v1/category/{id} ====================

    @Test
    @DisplayName("update_合法请求体_返回成功")
    void update_validRequest_returnsSuccess() throws Exception {
        doNothing().when(categoryService).update(eq(1L), any());

        mockMvc.perform(put("/api/v1/category/1")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(toJson(buildSaveRequest("全栈", null, null))))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.code").value(0));

        verify(categoryService).update(eq(1L), any());
    }

    @Test
    @DisplayName("update_分类名为空_返回参数校验失败")
    void update_blankName_returnsValidationError() throws Exception {
        mockMvc.perform(put("/api/v1/category/1")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(toJson(buildSaveRequest("", null, null))))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.code").value(40000))
                .andExpect(jsonPath("$.data").isEmpty());
    }

    // ==================== DELETE /api/v1/category/{id} ====================

    @Test
    @DisplayName("delete_正常请求_返回成功")
    void delete_normal_returnsSuccess() throws Exception {
        doNothing().when(categoryService).delete(1L);

        mockMvc.perform(delete("/api/v1/category/1"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.code").value(0));

        verify(categoryService).delete(1L);
    }

    // ==================== 辅助方法 ====================

    private CategorySaveRequest buildSaveRequest(String name, String description, Integer sortOrder) {
        CategorySaveRequest req = new CategorySaveRequest();
        req.setName(name);
        req.setDescription(description);
        req.setSortOrder(sortOrder);
        return req;
    }

    private String toJson(Object obj) throws Exception {
        return objectMapper.writeValueAsString(obj);
    }
}
