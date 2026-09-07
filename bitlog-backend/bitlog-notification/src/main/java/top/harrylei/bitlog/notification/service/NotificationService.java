package top.harrylei.bitlog.notification.service;

import top.harrylei.bitlog.common.model.PageVO;
import top.harrylei.bitlog.notification.model.dto.NotificationCreateDTO;
import top.harrylei.bitlog.notification.model.query.NotificationPageParam;
import top.harrylei.bitlog.notification.model.vo.NotificationVO;

import java.util.List;

/**
 * 通知模块内部服务
 *
 * @author Harry
 * @since 2026-09-06
 */
public interface NotificationService {

    /**
     * 派发一个业务事件产生的候选通知
     *
     * @param candidates 一个事件产生的候选通知
     */
    void dispatch(List<NotificationCreateDTO> candidates);

    /**
     * 分页查询我的通知
     *
     * @param userId 当前用户 ID
     * @param query 分页查询参数
     * @return 通知分页结果
     */
    PageVO<NotificationVO> pageNotifications(Long userId, NotificationPageParam query);

    /**
     * 统计我的未读通知数
     *
     * @param userId 当前用户 ID
     * @return 未读数
     */
    long countUnread(Long userId);

    /**
     * 标记一条我的通知为已读
     *
     * @param userId 当前用户 ID
     * @param notificationId 通知 ID
     */
    void markRead(Long userId, Long notificationId);

    /**
     * 标记我的全部通知为已读
     *
     * @param userId 当前用户 ID
     */
    void markAllRead(Long userId);
}
