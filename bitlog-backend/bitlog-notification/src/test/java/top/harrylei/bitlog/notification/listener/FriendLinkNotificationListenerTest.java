package top.harrylei.bitlog.notification.listener;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import java.util.List;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import top.harrylei.bitlog.link.event.FriendLinkAppliedEvent;
import top.harrylei.bitlog.link.event.FriendLinkReviewedEvent;
import top.harrylei.bitlog.link.model.enums.FriendLinkStatusEnum;
import top.harrylei.bitlog.notification.model.dto.NotificationCreateDTO;
import top.harrylei.bitlog.notification.port.NotificationPort;
import top.harrylei.bitlog.user.port.UserPort;

/**
 * FriendLinkNotificationListener 单元测试
 *
 * @author Harry
 * @since 2026-09-07
 */
@ExtendWith(MockitoExtension.class)
class FriendLinkNotificationListenerTest {

    @Mock
    private NotificationPort notificationPort;

    @Mock
    private UserPort userPort;

    private FriendLinkNotificationListener listener;

    @BeforeEach
    void setUp() {
        listener = new FriendLinkNotificationListener(notificationPort, userPort);
    }

    @Test
    @DisplayName("onFriendLinkApplied_存在两个管理员_派发两条候选通知")
    void onFriendLinkApplied_twoAdmins_dispatchesTwoCandidates() {
        when(userPort.listAdminIds()).thenReturn(List.of(10L, 11L));
        FriendLinkAppliedEvent event =
                new FriendLinkAppliedEvent(100L, 2L, "BitLog", "https://bitlog.example.com", "申请留言");

        listener.onFriendLinkApplied(event);

        ArgumentCaptor<List<NotificationCreateDTO>> captor = ArgumentCaptor.forClass(List.class);
        verify(notificationPort).dispatch(captor.capture());
        assertThat(captor.getValue())
                .hasSize(2)
                .extracting(NotificationCreateDTO::recipientId)
                .containsExactly(10L, 11L);
        assertThat(captor.getValue())
                .extracting(NotificationCreateDTO::dedupeKey)
                .containsOnly("link_applied:100");
    }

    @Test
    @DisplayName("onFriendLinkApplied_没有管理员_不派发通知")
    void onFriendLinkApplied_noAdmins_doesNotDispatch() {
        when(userPort.listAdminIds()).thenReturn(List.of());
        FriendLinkAppliedEvent event =
                new FriendLinkAppliedEvent(100L, 2L, "BitLog", "https://bitlog.example.com", null);

        listener.onFriendLinkApplied(event);

        verify(notificationPort, never()).dispatch(org.mockito.ArgumentMatchers.anyList());
    }

    @Test
    @DisplayName("onFriendLinkReviewed_没有申请人_不派发通知")
    void onFriendLinkReviewed_ownerIdIsNull_doesNotDispatch() {
        FriendLinkReviewedEvent event =
                new FriendLinkReviewedEvent(100L, null, 1L, FriendLinkStatusEnum.APPROVED, "BitLog", null);

        listener.onFriendLinkReviewed(event);

        verify(notificationPort, never()).dispatch(org.mockito.ArgumentMatchers.anyList());
    }

    @Test
    @DisplayName("onFriendLinkReviewed_审核完成_派发不去重的审核通知")
    void onFriendLinkReviewed_normalEvent_dispatchesNonDedupedCandidate() {
        FriendLinkReviewedEvent event =
                new FriendLinkReviewedEvent(100L, 2L, 1L, FriendLinkStatusEnum.REJECTED, "BitLog", "未找到回链");

        listener.onFriendLinkReviewed(event);

        ArgumentCaptor<List<NotificationCreateDTO>> captor = ArgumentCaptor.forClass(List.class);
        verify(notificationPort).dispatch(captor.capture());
        assertThat(captor.getValue()).singleElement().satisfies(candidate -> {
            assertThat(candidate.dedupeKey()).isNull();
            assertThat(candidate.payload()).containsEntry("status", FriendLinkStatusEnum.REJECTED.getCode());
        });
    }
}
