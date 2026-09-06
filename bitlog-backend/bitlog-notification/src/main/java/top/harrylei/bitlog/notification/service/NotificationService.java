package top.harrylei.bitlog.notification.service;

import top.harrylei.bitlog.notification.model.dto.NotificationCreateDTO;

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
}
