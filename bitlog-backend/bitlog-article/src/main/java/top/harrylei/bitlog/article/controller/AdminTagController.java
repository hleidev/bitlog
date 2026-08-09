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
import top.harrylei.bitlog.api.model.article.req.TagBatchDeleteParam;
import top.harrylei.bitlog.api.model.article.req.TagSaveParam;
import top.harrylei.bitlog.api.model.article.req.TagUpdateParam;
import top.harrylei.bitlog.article.service.TagService;
import top.harrylei.bitlog.common.model.Result;
import top.harrylei.bitlog.common.security.RequiresAdmin;

/**
 * 管理员标签接口，与 {@link TagController} 共用前缀
 *
 * @author Harry
 * @since 2026-08-09
 */
@Tag(name = "管理员标签接口")
@RequiresAdmin
@RestController
@RequestMapping("/api/v1/tag")
@RequiredArgsConstructor
public class AdminTagController {

    private final TagService tagService;

    @Operation(summary = "查询或创建标签")
    @PostMapping("/get-or-create")
    public Result<Long> getOrCreate(@RequestParam String name) {
        return Result.success(tagService.getOrCreate(name));
    }

    @Operation(summary = "创建标签")
    @PostMapping
    public Result<Long> create(@Valid @RequestBody TagSaveParam req) {
        return Result.success(tagService.save(req));
    }

    @Operation(summary = "更新标签")
    @PutMapping("/{id}")
    public Result<Void> update(@PathVariable Long id, @Valid @RequestBody TagUpdateParam req) {
        tagService.update(id, req);
        return Result.success();
    }

    @Operation(summary = "批量删除标签")
    @DeleteMapping
    public Result<Void> batchDelete(@Valid @RequestBody TagBatchDeleteParam req) {
        tagService.batchDelete(req.getIds());
        return Result.success();
    }
}
