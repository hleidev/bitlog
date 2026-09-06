package top.harrylei.bitlog.notification.service.impl;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import top.harrylei.bitlog.notification.model.enums.NotificationTargetTypeEnum;
import top.harrylei.bitlog.notification.model.enums.NotificationTypeEnum;
import top.harrylei.bitlog.notification.model.dto.NotificationCreateDTO;
import top.harrylei.bitlog.notification.repository.dao.NotificationDAO;
import top.harrylei.bitlog.notification.repository.entity.NotificationDO;
import top.harrylei.bitlog.notification.repository.mapper.NotificationMapper;

import java.util.List;
import java.util.Map;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoInteractions;
import static org.mockito.Mockito.when;

/**
 * NotificationServiceImpl 单元测试
 *
 * @author Harry
 * @since 2026-09-06
 */
@ExtendWith(MockitoExtension.class)
class NotificationServiceImplTest {

    @Mock
    private NotificationDAO notificationDAO;

    @Mock
    private NotificationMapper notificationMapper;

    private NotificationServiceImpl notificationService;

    @BeforeEach
    void setUp() {
        notificationService = new NotificationServiceImpl(notificationDAO);
    }

    private NotificationCreateDTO command(Long recipientId, NotificationTypeEnum type, Long actorId, Long targetId) {
        return new NotificationCreateDTO(recipientId, type, actorId, NotificationTargetTypeEnum.COMMENT, targetId,
            Map.of());
    }

    @Test
    @DisplayName("dispatch_收件人与触发者相同_被丢弃不落库")
    void dispatch_recipientEqualsActor_isDiscarded() {
        NotificationCreateDTO self = command(1L, NotificationTypeEnum.COMMENT_REPLY, 1L, 100L);

        notificationService.dispatch(List.of(self));

        verifyNoInteractions(notificationDAO);
    }

    @Test
    @DisplayName("dispatch_同批同收件人命中两种类型_只落优先级高的一条")
    void dispatch_sameRecipientTwoTypes_keepsHigherPriorityOnly() {
        when(notificationDAO.getBaseMapper()).thenReturn(notificationMapper);
        NotificationCreateDTO reply = command(1L, NotificationTypeEnum.COMMENT_REPLY, 2L, 100L);
        NotificationCreateDTO articleComment = command(1L, NotificationTypeEnum.ARTICLE_COMMENT, 3L, 100L);

        notificationService.dispatch(List.of(articleComment, reply));

        ArgumentCaptor<NotificationDO> captor = ArgumentCaptor.forClass(NotificationDO.class);
        verify(notificationMapper, times(1)).insertIgnoreDuplicate(captor.capture());
        assertThat(captor.getValue().getType()).isEqualTo(NotificationTypeEnum.COMMENT_REPLY);
    }

    @Test
    @DisplayName("dispatch_不同收件人各一条_互不影响各自落库")
    void dispatch_differentRecipients_eachDispatchedIndependently() {
        when(notificationDAO.getBaseMapper()).thenReturn(notificationMapper);
        NotificationCreateDTO toA = command(1L, NotificationTypeEnum.COMMENT_REPLY, 9L, 100L);
        NotificationCreateDTO toB = command(2L, NotificationTypeEnum.ARTICLE_COMMENT, 9L, 100L);

        notificationService.dispatch(List.of(toA, toB));

        verify(notificationMapper, times(2)).insertIgnoreDuplicate(any(NotificationDO.class));
    }

    @Test
    @DisplayName("dispatch_空列表_不触碰mapper")
    void dispatch_emptyList_doesNotTouchMapper() {
        notificationService.dispatch(List.of());

        verifyNoInteractions(notificationDAO);
    }

    @Test
    @DisplayName("dispatch_全部被自我抑制_不触碰mapper")
    void dispatch_allSelfSuppressed_doesNotTouchMapper() {
        NotificationCreateDTO selfA = command(1L, NotificationTypeEnum.COMMENT_REPLY, 1L, 100L);
        NotificationCreateDTO selfB = command(2L, NotificationTypeEnum.ARTICLE_COMMENT, 2L, 101L);

        notificationService.dispatch(List.of(selfA, selfB));

        verifyNoInteractions(notificationDAO);
    }
}
