package top.harrylei.bitlog.article.controller;

import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.*;
import top.harrylei.bitlog.api.model.article.req.CategorySaveRequest;
import top.harrylei.bitlog.api.model.article.vo.CategoryVO;
import top.harrylei.bitlog.article.service.CategoryService;
import top.harrylei.bitlog.common.model.Result;

import java.util.List;

/**
 * 分类接口
 *
 * @author harry
 * @since 0.0.1
 */
@Tag(name = "分类接口")
@RestController
@RequestMapping("/api/v1/category")
@RequiredArgsConstructor
public class CategoryController {

    private final CategoryService categoryService;

    @Operation(summary = "查询所有分类（按使用频率降序）")
    @GetMapping
    public Result<List<CategoryVO>> listAll() {
        return Result.success(categoryService.listAll());
    }

    @Operation(summary = "查询或创建分类")
    @PostMapping("/get-or-create")
    public Result<Long> getOrCreate(@RequestParam String name) {
        return Result.success(categoryService.getOrCreate(name));
    }

    @Operation(summary = "创建分类")
    @PostMapping
    public Result<Long> create(@Valid @RequestBody CategorySaveRequest req) {
        return Result.success(categoryService.save(req));
    }

    @Operation(summary = "更新分类")
    @PutMapping("/{id}")
    public Result<Void> update(@PathVariable Long id, @Valid @RequestBody CategorySaveRequest req) {
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
