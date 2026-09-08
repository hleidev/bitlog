package top.harrylei.bitlog.notification.service.impl;

import com.baomidou.mybatisplus.core.metadata.IPage;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Set;
import java.util.stream.Collectors;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import top.harrylei.bitlog.common.enums.ResultCode;
import top.harrylei.bitlog.common.model.PageVO;
import top.harrylei.bitlog.notification.converter.NotificationConverter;
import top.harrylei.bitlog.notification.model.dto.NotificationCreateDTO;
import top.harrylei.bitlog.notification.model.query.NotificationPageParam;
import top.harrylei.bitlog.notification.model.vo.NotificationActorVO;
import top.harrylei.bitlog.notification.model.vo.NotificationVO;
import top.harrylei.bitlog.notification.repository.dao.NotificationDAO;
import top.harrylei.bitlog.notification.repository.entity.NotificationDO;
import top.harrylei.bitlog.notification.service.NotificationService;
import top.harrylei.bitlog.user.model.vo.UserVO;
import top.harrylei.bitlog.user.port.UserPort;

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
    private final NotificationConverter notificationConverter;
    private final UserPort userPort;

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
            deduped.merge(
                    candidate.recipientId(),
                    candidate,
                    (kept, incoming) ->
                            incoming.type().getPriority() > kept.type().getPriority() ? incoming : kept);
        }

        for (NotificationCreateDTO candidate : deduped.values()) {
            notificationDAO.getBaseMapper().insertIgnoreDuplicate(toDO(candidate));
        }
    }

    @Override
    public PageVO<NotificationVO> pageNotifications(Long userId, NotificationPageParam query) {
        IPage<NotificationDO> page = notificationDAO.pageByRecipient(userId, query.getUnreadOnly(), query.toPage());
        List<NotificationDO> records = page.getRecords();
        if (records.isEmpty()) {
            return PageVO.of(page, List.of());
        }

        Map<Long, NotificationActorVO> actorMap = loadActorMap(records);
        List<NotificationVO> content = records.stream()
                .map(notification -> {
                    NotificationVO vo = notificationConverter.toVO(notification);
                    if (notification.getActorId() != null) {
                        vo.setActor(actorMap.get(notification.getActorId()));
                    }
                    return vo;
                })
                .toList();
        return PageVO.of(page, content);
    }

    @Override
    public long countUnread(Long userId) {
        return notificationDAO.countUnread(userId);
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public void markRead(Long userId, Long notificationId) {
        if (notificationDAO.markRead(notificationId, userId) == 0) {
            throw ResultCode.NOTIFICATION_NOT_EXISTS.toException();
        }
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public void markAllRead(Long userId) {
        notificationDAO.markAllRead(userId);
    }

    private Map<Long, NotificationActorVO> loadActorMap(List<NotificationDO> notifications) {
        Set<Long> actorIds = notifications.stream()
                .map(NotificationDO::getActorId)
                .filter(Objects::nonNull)
                .collect(Collectors.toSet());
        if (actorIds.isEmpty()) {
            return Map.of();
        }
        return userPort.getUserBatchByIds(List.copyOf(actorIds)).stream()
                .collect(Collectors.toMap(UserVO::getUserId, notificationConverter::toActorVO, (a, b) -> a));
    }

    private NotificationDO toDO(NotificationCreateDTO candidate) {
        return new NotificationDO()
                .setRecipientId(candidate.recipientId())
                .setType(candidate.type())
                .setActorId(candidate.actorId())
                .setTargetType(candidate.targetType())
                .setTargetId(candidate.targetId())
                .setDedupeKey(candidate.dedupeKey())
                .setPayload(candidate.payload() != null ? candidate.payload() : Map.of());
    }
}
