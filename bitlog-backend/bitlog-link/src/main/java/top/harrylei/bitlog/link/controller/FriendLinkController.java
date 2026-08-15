package top.harrylei.bitlog.link.controller;

import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;
import top.harrylei.bitlog.api.model.link.vo.FriendLinkVO;
import top.harrylei.bitlog.common.model.Result;
import top.harrylei.bitlog.link.service.FriendLinkService;

import java.util.List;

/**
 * 友链接口，仅公开读取，管理操作见 AdminFriendLinkController
 *
 * @author Harry
 * @since 2026-08-14
 */
@Tag(name = "友链接口")
@RestController
@RequestMapping("/api/v1/links")
@RequiredArgsConstructor
public class FriendLinkController {

    private final FriendLinkService friendLinkService;

    @Operation(summary = "查询公开展示的友链")
    @GetMapping
    public Result<List<FriendLinkVO>> listApproved() {
        return Result.success(friendLinkService.listApproved());
    }
}
