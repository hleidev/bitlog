package top.harrylei.bitlog.article.controller;

import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;
import top.harrylei.bitlog.api.model.article.req.CategoryCreateParam;
import top.harrylei.bitlog.api.model.article.req.CategoryUpdateParam;
import top.harrylei.bitlog.article.service.CategoryService;
import top.harrylei.bitlog.common.model.Result;
import top.harrylei.bitlog.common.security.RequiresAdmin;

/**
 * 管理员分类接口，与 {@link CategoryController} 共用前缀
 *
 * @author Harry
 * @since 2026-08-09
 */
@Tag(name = "管理员分类接口")
@RequiresAdmin
@RestController
@RequestMapping("/api/v1/category")
@RequiredArgsConstructor
public class AdminCategoryController {

    private final CategoryService categoryService;

    @Operation(summary = "查询或创建顶级分类")
    @PostMapping("/get-or-create")
    public Result<Long> getOrCreate(@RequestParam String name) {
        return Result.success(categoryService.getOrCreate(name));
    }

    @Operation(summary = "创建分类")
    @PostMapping
    public Result<Long> create(@Valid @RequestBody CategoryCreateParam req) {
        return Result.success(categoryService.save(req));
    }

    @Operation(summary = "更新分类名称")
    @PutMapping("/{id}")
    public Result<Void> update(@PathVariable Long id, @Valid @RequestBody CategoryUpdateParam req) {
        categoryService.update(id, req);
        return Result.success();
    }

    @Operation(summary = "删除分类")
    @DeleteMapping("/{id}")
    public Result<Void> delete(@PathVariable Long id) {
        categoryService.delete(id);
        return Result.success();
    }
}
