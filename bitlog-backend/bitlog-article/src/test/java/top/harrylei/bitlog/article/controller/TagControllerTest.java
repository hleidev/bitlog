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
import top.harrylei.bitlog.api.model.article.req.TagBatchDeleteRequest;
import top.harrylei.bitlog.api.model.article.req.TagSaveRequest;
import top.harrylei.bitlog.api.model.article.req.TagUpdateRequest;
import top.harrylei.bitlog.api.model.article.vo.TagVO;
import top.harrylei.bitlog.article.service.TagService;
import top.harrylei.bitlog.common.advice.GlobalExceptionHandler;

import java.util.List;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.ArgumentMatchers.isNull;
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
@DisplayName("TagController 标签接口测试")
class TagControllerTest {

    @Mock
    private TagService tagService;

    @InjectMocks
    private TagController tagController;

    private MockMvc mockMvc;
    private final ObjectMapper objectMapper = new ObjectMapper();

    @BeforeEach
    void setUp() {
        mockMvc = MockMvcBuilders
                .standaloneSetup(tagController)
                .setControllerAdvice(new GlobalExceptionHandler())
                .build();
    }

    // ==================== GET /api/v1/tag ====================

    @Test
    @DisplayName("listAll_无搜索词_返回全量标签列表")
    void listAll_noName_returnsAllTags() throws Exception {
        TagVO vo = new TagVO().setId(1L).setName("Java").setArticleCount(10);
        when(tagService.listAll(isNull())).thenReturn(List.of(vo));

        mockMvc.perform(get("/api/v1/tag"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.code").value(0))
                .andExpect(jsonPath("$.data[0].id").value(1))
                .andExpect(jsonPath("$.data[0].name").value("Java"));
    }

    @Test
    @DisplayName("listAll_带搜索词_按名称模糊过滤")
    void listAll_withName_filtersResults() throws Exception {
        TagVO vo = new TagVO().setId(1L).setName("Java").setArticleCount(10);
        when(tagService.listAll("Java")).thenReturn(List.of(vo));

        mockMvc.perform(get("/api/v1/tag").param("name", "Java"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.code").value(0))
                .andExpect(jsonPath("$.data[0].name").value("Java"));

        verify(tagService).listAll("Java");
    }

    // ==================== POST /api/v1/tag/get-or-create ====================

    @Test
    @DisplayName("getOrCreate_正常请求_返回标签ID")
    void getOrCreate_normal_returnsId() throws Exception {
        when(tagService.getOrCreate("Java")).thenReturn(1L);

        mockMvc.perform(post("/api/v1/tag/get-or-create")
                        .param("name", "Java"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.code").value(0))
                .andExpect(jsonPath("$.data").value(1));
    }

    // ==================== POST /api/v1/tag ====================

    @Test
    @DisplayName("create_合法请求体_返回新标签ID")
    void save_validRequest_returnsId() throws Exception {
        when(tagService.save(any())).thenReturn(2L);

        mockMvc.perform(post("/api/v1/tag")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(toJson(saveRequest("Go"))))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.code").value(0))
                .andExpect(jsonPath("$.data").value(2));
    }

    @Test
    @DisplayName("create_标签名为空_返回参数校验失败")
    void save_blankName_returnsValidationError() throws Exception {
        mockMvc.perform(post("/api/v1/tag")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(toJson(saveRequest(""))))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.code").value(40000))
                .andExpect(jsonPath("$.data").isEmpty());
    }

    // ==================== PUT /api/v1/tag/{id} ====================

    @Test
    @DisplayName("update_合法请求体_返回成功")
    void update_validRequest_returnsSuccess() throws Exception {
        doNothing().when(tagService).update(eq(1L), any());

        mockMvc.perform(put("/api/v1/tag/1")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(toJson(updateRequest("Golang"))))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.code").value(0));

        verify(tagService).update(eq(1L), any());
    }

    @Test
    @DisplayName("update_标签名为空_返回参数校验失败")
    void update_blankName_returnsValidationError() throws Exception {
        mockMvc.perform(put("/api/v1/tag/1")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(toJson(updateRequest(""))))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.code").value(40000))
                .andExpect(jsonPath("$.data").isEmpty());
    }

    // ==================== DELETE /api/v1/tag ====================

    @Test
    @DisplayName("batchDelete_合法请求_返回成功")
    void batchDelete_validRequest_returnsSuccess() throws Exception {
        doNothing().when(tagService).batchDelete(List.of(1L, 2L));

        mockMvc.perform(delete("/api/v1/tag")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(toJson(batchDeleteRequest(List.of(1L, 2L)))))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.code").value(0));

        verify(tagService).batchDelete(List.of(1L, 2L));
    }

    @Test
    @DisplayName("batchDelete_ids为空_返回参数校验失败")
    void batchDelete_emptyIds_returnsValidationError() throws Exception {
        mockMvc.perform(delete("/api/v1/tag")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(toJson(batchDeleteRequest(List.of()))))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.code").value(40000))
                .andExpect(jsonPath("$.data").isEmpty());
    }

    // ==================== 辅助方法 ====================

    private TagSaveRequest saveRequest(String name) {
        TagSaveRequest req = new TagSaveRequest();
        req.setName(name);
        return req;
    }

    private TagUpdateRequest updateRequest(String name) {
        TagUpdateRequest req = new TagUpdateRequest();
        req.setName(name);
        return req;
    }

    private TagBatchDeleteRequest batchDeleteRequest(List<Long> ids) {
        TagBatchDeleteRequest req = new TagBatchDeleteRequest();
        req.setIds(ids);
        return req;
    }

    private String toJson(Object obj) throws Exception {
        return objectMapper.writeValueAsString(obj);
    }
}
