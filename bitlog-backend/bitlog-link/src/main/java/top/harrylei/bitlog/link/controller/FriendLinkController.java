package top.harrylei.bitlog.link.controller;

import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;
import top.harrylei.bitlog.api.model.link.req.FriendLinkSaveParam;
import top.harrylei.bitlog.api.model.link.vo.FriendLinkVO;
import top.harrylei.bitlog.api.model.link.vo.MyFriendLinkVO;
import top.harrylei.bitlog.common.context.ReqInfoContext;
import top.harrylei.bitlog.common.model.Result;
import top.harrylei.bitlog.common.security.RequiresLogin;
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

    @Operation(summary = "查询我的友链，未申请时返回空")
    @RequiresLogin
    @GetMapping("/mine")
    public Result<MyFriendLinkVO> getMine() {
        return Result.success(friendLinkService.getMine(currentUserId()));
    }

    @Operation(summary = "提交友链申请")
    @RequiresLogin
    @PostMapping("/mine")
    public Result<Long> applyMine(@Valid @RequestBody FriendLinkSaveParam param) {
        return Result.success(friendLinkService.applyMine(currentUserId(), param));
    }

    @Operation(summary = "修改我的友链")
    @RequiresLogin
    @PutMapping("/mine")
    public Result<Void> updateMine(@Valid @RequestBody FriendLinkSaveParam param) {
        friendLinkService.updateMine(currentUserId(), param);
        return Result.success();
    }

    @Operation(summary = "撤回或删除我的友链")
    @RequiresLogin
    @DeleteMapping("/mine")
    public Result<Void> deleteMine() {
        friendLinkService.deleteMine(currentUserId());
        return Result.success();
    }

    private Long currentUserId() {
        return ReqInfoContext.getContext().getUserId();
    }
}
