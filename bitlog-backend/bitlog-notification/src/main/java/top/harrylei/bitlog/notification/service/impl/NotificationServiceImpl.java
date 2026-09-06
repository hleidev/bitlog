package top.harrylei.bitlog.notification.service.impl;

import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import top.harrylei.bitlog.notification.model.dto.NotificationCreateDTO;
import top.harrylei.bitlog.notification.repository.dao.NotificationDAO;
import top.harrylei.bitlog.notification.repository.entity.NotificationDO;
import top.harrylei.bitlog.notification.service.NotificationService;

import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Objects;

/**
 * 通知模块内部服务实现
 *
 * @author Harry
 * @since 2026-09-06
 */
@Service
@RequiredArgsConstructor
public class NotificationServiceImpl implements NotificationService {

    private final NotificationDAO notificationDAO;

    @Override
    @Transactional(rollbackFor = Exception.class)
    public void dispatch(List<NotificationCreateDTO> candidates) {
        if (candidates == null || candidates.isEmpty()) {
            return;
        }

        Map<Long, NotificationCreateDTO> deduped = new LinkedHashMap<>();
        for (NotificationCreateDTO candidate : candidates) {
            if (Objects.equals(candidate.recipientId(), candidate.actorId())) {
                // 自我抑制：自己的动作不该通知自己
                continue;
            }
            deduped.merge(candidate.recipientId(), candidate,
                (kept, incoming) -> incoming.type().getPriority() > kept.type().getPriority() ? incoming : kept);
        }

        for (NotificationCreateDTO candidate : deduped.values()) {
            notificationDAO.getBaseMapper().insertIgnoreDuplicate(toDO(candidate));
        }
    }

    private NotificationDO toDO(NotificationCreateDTO candidate) {
        return new NotificationDO().setRecipientId(candidate.recipientId()).setType(candidate.type())
            .setActorId(candidate.actorId()).setTargetType(candidate.targetType()).setTargetId(candidate.targetId())
            .setPayload(candidate.payload() != null ? candidate.payload() : Map.of());
    }
}
