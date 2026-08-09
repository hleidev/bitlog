package top.harrylei.bitlog.article.controller;

import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.*;
import top.harrylei.bitlog.api.model.article.vo.CategoryVO;
import top.harrylei.bitlog.article.service.CategoryService;
import top.harrylei.bitlog.common.model.Result;

import java.util.List;

/**
 * 分类接口，仅公开读取，写操作见 {@link AdminCategoryController}
 *
 * @author Harry
 * @since 2026-04-02
 */
@Tag(name = "分类接口")
@RestController
@RequestMapping("/api/v1/category")
@RequiredArgsConstructor
public class CategoryController {

    private final CategoryService categoryService;

    @Operation(summary = "查询所有分类，支持按名称模糊搜索")
    @GetMapping
    public Result<List<CategoryVO>> listAll(@RequestParam(required = false) String name) {
        return Result.success(categoryService.listAll(name));
    }
}
