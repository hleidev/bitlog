package top.harrylei.bitlog.article.controller;

import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.*;
import top.harrylei.bitlog.api.model.article.req.TagBatchDeleteRequest;
import top.harrylei.bitlog.api.model.article.req.TagSaveRequest;
import top.harrylei.bitlog.api.model.article.req.TagUpdateRequest;
import top.harrylei.bitlog.api.model.article.vo.TagVO;
import top.harrylei.bitlog.article.service.TagService;
import top.harrylei.bitlog.common.model.Result;
import top.harrylei.bitlog.common.security.RequiresAdmin;
import top.harrylei.bitlog.common.security.RequiresLogin;

import java.util.List;

/**
 * 标签接口
 *
 * @author Harry
 * @since 2026-04-02
 */
@Tag(name = "标签接口")
@RestController
@RequestMapping("/api/v1/tag")
@RequiredArgsConstructor
public class TagController {

    private final TagService tagService;

    @Operation(summary = "查询所有标签")
    @GetMapping
    public Result<List<TagVO>> listAll(@RequestParam(required = false) String name) {
        return Result.success(tagService.listAll(name));
    }

    @RequiresLogin
    @Operation(summary = "查询或创建标签")
    @PostMapping("/get-or-create")
    public Result<Long> getOrCreate(@RequestParam String name) {
        return Result.success(tagService.getOrCreate(name));
    }

    @RequiresAdmin
    @Operation(summary = "创建标签")
    @PostMapping
    public Result<Long> create(@Valid @RequestBody TagSaveRequest req) {
        return Result.success(tagService.save(req));
    }

    @RequiresAdmin
    @Operation(summary = "更新标签")
    @PutMapping("/{id}")
    public Result<Void> update(@PathVariable Long id, @Valid @RequestBody TagUpdateRequest req) {
        tagService.update(id, req);
        return Result.success();
    }

    @RequiresAdmin
    @Operation(summary = "批量删除标签")
    @DeleteMapping
    public Result<Void> batchDelete(@Valid @RequestBody TagBatchDeleteRequest req) {
        tagService.batchDelete(req.getIds());
        return Result.success();
    }
}
