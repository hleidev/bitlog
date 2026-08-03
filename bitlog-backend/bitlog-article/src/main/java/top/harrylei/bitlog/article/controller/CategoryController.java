package top.harrylei.bitlog.article.controller;

import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.*;
import top.harrylei.bitlog.api.model.article.req.CategoryCreateParam;
import top.harrylei.bitlog.api.model.article.req.CategoryUpdateParam;
import top.harrylei.bitlog.api.model.article.vo.CategoryVO;
import top.harrylei.bitlog.article.service.CategoryService;
import top.harrylei.bitlog.common.model.Result;
import top.harrylei.bitlog.common.security.RequiresAdmin;

import java.util.List;

/**
 * 分类接口
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

    @RequiresAdmin
    @Operation(summary = "查询或创建顶级分类")
    @PostMapping("/get-or-create")
    public Result<Long> getOrCreate(@RequestParam String name) {
        return Result.success(categoryService.getOrCreate(name));
    }

    @RequiresAdmin
    @Operation(summary = "创建分类")
    @PostMapping
    public Result<Long> create(@Valid @RequestBody CategoryCreateParam req) {
        return Result.success(categoryService.save(req));
    }

    @RequiresAdmin
    @Operation(summary = "更新分类名称")
    @PutMapping("/{id}")
    public Result<Void> update(@PathVariable Long id, @Valid @RequestBody CategoryUpdateParam req) {
        categoryService.update(id, req);
        return Result.success();
    }

    @RequiresAdmin
    @Operation(summary = "删除分类")
    @DeleteMapping("/{id}")
    public Result<Void> delete(@PathVariable Long id) {
        categoryService.delete(id);
        return Result.success();
    }
}
