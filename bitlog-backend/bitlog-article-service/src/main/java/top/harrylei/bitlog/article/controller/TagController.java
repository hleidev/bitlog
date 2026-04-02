package top.harrylei.bitlog.article.controller;

import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.*;
import top.harrylei.bitlog.api.model.article.req.TagSaveRequest;
import top.harrylei.bitlog.api.model.article.req.TagUpdateRequest;
import top.harrylei.bitlog.api.model.article.vo.TagVO;
import top.harrylei.bitlog.article.service.TagService;
import top.harrylei.bitlog.common.model.Result;

import java.util.List;

/**
 * 标签接口
 *
 * @author harry
 * @since 0.0.1
 */
@Tag(name = "标签接口")
@RestController
@RequestMapping("/api/v1/tag")
@RequiredArgsConstructor
public class TagController {

    private final TagService tagService;

    @Operation(summary = "查询所有标签（按使用频率降序）")
    @GetMapping
    public Result<List<TagVO>> listAll() {
        return Result.success(tagService.listAll());
    }

    @Operation(summary = "查询或创建标签")
    @PostMapping("/get-or-create")
    public Result<Long> getOrCreate(@RequestParam String name) {
        return Result.success(tagService.getOrCreate(name));
    }

    @Operation(summary = "创建标签")
    @PostMapping
    public Result<Long> create(@Valid @RequestBody TagSaveRequest req) {
        return Result.success(tagService.save(req));
    }

    @Operation(summary = "更新标签")
    @PutMapping("/{id}")
    public Result<Void> update(@PathVariable Long id, @Valid @RequestBody TagUpdateRequest req) {
        tagService.update(id, req);
        return Result.success();
    }

    @Operation(summary = "删除标签")
    @DeleteMapping("/{id}")
    public Result<Void> delete(@PathVariable Long id) {
        tagService.delete(id);
        return Result.success();
    }
}
