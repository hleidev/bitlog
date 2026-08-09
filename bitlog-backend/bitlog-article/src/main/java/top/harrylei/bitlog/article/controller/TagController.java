package top.harrylei.bitlog.article.controller;

import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.*;
import top.harrylei.bitlog.api.model.article.vo.TagVO;
import top.harrylei.bitlog.article.service.TagService;
import top.harrylei.bitlog.common.model.Result;

import java.util.List;

/**
 * 标签接口，仅公开读取，写操作见 {@link AdminTagController}
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
}
