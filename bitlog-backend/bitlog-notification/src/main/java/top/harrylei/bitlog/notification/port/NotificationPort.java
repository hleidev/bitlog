package top.harrylei.bitlog.notification.port;

import top.harrylei.bitlog.notification.model.dto.NotificationCreateDTO;

import java.util.List;

/**
 * 通知模块对外契约，其余模块只应依赖本接口，不直接注入领域服务
 *
 * @author Harry
 * @since 2026-09-06
 */
public interface NotificationPort {

    /**
     * 派发一个业务事件产生的候选通知
     *
     * @param candidates 一个事件产生的候选通知
     */
    void dispatch(List<NotificationCreateDTO> candidates);
}
