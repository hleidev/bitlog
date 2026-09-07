package top.harrylei.bitlog.notification.port.impl;

import java.util.List;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import top.harrylei.bitlog.notification.model.dto.NotificationCreateDTO;
import top.harrylei.bitlog.notification.port.NotificationPort;
import top.harrylei.bitlog.notification.service.NotificationService;

/**
 * 通知模块对外契约实现
 *
 * @author Harry
 * @since 2026-09-06
 */
@Service
@RequiredArgsConstructor
public class NotificationPortImpl implements NotificationPort {

    private final NotificationService notificationService;

    @Override
    public void dispatch(List<NotificationCreateDTO> candidates) {
        notificationService.dispatch(candidates);
    }
}
