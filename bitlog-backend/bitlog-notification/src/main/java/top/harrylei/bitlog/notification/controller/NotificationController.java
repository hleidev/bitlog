package top.harrylei.bitlog.notification.controller;

import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PatchMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;
import top.harrylei.bitlog.common.context.ReqInfoContext;
import top.harrylei.bitlog.common.model.PageVO;
import top.harrylei.bitlog.common.model.Result;
import top.harrylei.bitlog.common.security.RequiresLogin;
import top.harrylei.bitlog.notification.model.query.NotificationPageParam;
import top.harrylei.bitlog.notification.model.vo.NotificationVO;
import top.harrylei.bitlog.notification.service.NotificationService;

/**
 * 通知接口
 *
 * @author Harry
 * @since 2026-09-07
 */
@Tag(name = "通知接口")
@RestController
@RequestMapping("/api/v1/notifications")
@RequiredArgsConstructor
public class NotificationController {

    private final NotificationService notificationService;

    @RequiresLogin
    @Operation(summary = "分页查询我的通知")
    @GetMapping("/page")
    public Result<PageVO<NotificationVO>> page(@Valid NotificationPageParam query) {
        return Result.success(notificationService.pageNotifications(
                ReqInfoContext.getContext().getUserId(), query));
    }

    @RequiresLogin
    @Operation(summary = "查询我的未读通知数")
    @GetMapping("/unread-count")
    public Result<Long> unreadCount() {
        return Result.success(
                notificationService.countUnread(ReqInfoContext.getContext().getUserId()));
    }

    @RequiresLogin
    @Operation(summary = "标记一条我的通知为已读")
    @PatchMapping("/{notificationId}/read")
    public Result<Void> markRead(@PathVariable Long notificationId) {
        notificationService.markRead(ReqInfoContext.getContext().getUserId(), notificationId);
        return Result.success();
    }

    @RequiresLogin
    @Operation(summary = "标记我的全部通知为已读")
    @PatchMapping("/read-all")
    public Result<Void> markAllRead() {
        notificationService.markAllRead(ReqInfoContext.getContext().getUserId());
        return Result.success();
    }
}
