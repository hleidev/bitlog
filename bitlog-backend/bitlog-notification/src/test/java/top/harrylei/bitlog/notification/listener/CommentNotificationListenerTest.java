package top.harrylei.bitlog.notification.listener;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import java.util.List;
import java.util.Map;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import top.harrylei.bitlog.article.port.ArticlePort;
import top.harrylei.bitlog.comment.event.CommentCreatedEvent;
import top.harrylei.bitlog.notification.model.dto.NotificationCreateDTO;
import top.harrylei.bitlog.notification.port.NotificationPort;

/**
 * CommentNotificationListener 单元测试
 *
 * @author Harry
 * @since 2026-09-06
 */
@ExtendWith(MockitoExtension.class)
class CommentNotificationListenerTest {

    private static final Long ARTICLE_ID = 1L;

    @Mock
    private NotificationPort notificationPort;

    @Mock
    private ArticlePort articlePort;

    private CommentNotificationListener listener;

    @BeforeEach
    void setUp() {
        listener = new CommentNotificationListener(notificationPort, articlePort);
        when(articlePort.getArticleTitles(List.of(ARTICLE_ID))).thenReturn(Map.of(ARTICLE_ID, "标题"));
        when(articlePort.getAuthorId(ARTICLE_ID)).thenReturn(9L);
    }

    private String dispatchAndCaptureSummary(String content) {
        CommentCreatedEvent event = new CommentCreatedEvent(100L, ARTICLE_ID, 2L, null, content);
        listener.onCommentCreated(event);

        ArgumentCaptor<List<NotificationCreateDTO>> captor = ArgumentCaptor.forClass(List.class);
        verify(notificationPort).dispatch(captor.capture());
        return (String) captor.getValue().get(0).payload().get("commentSummary");
    }

    @Test
    @DisplayName("onCommentCreated_内容不超过80字符_摘要原样保留")
    void onCommentCreated_contentWithinLimit_summaryUnchanged() {
        String content = "hello world";

        String summary = dispatchAndCaptureSummary(content);

        assertThat(summary).isEqualTo(content);
    }

    @Test
    @DisplayName("onCommentCreated_第80位落在emoji代理对中间_摘要不切出孤立代理项")
    void onCommentCreated_boundaryInsideSurrogatePair_summaryHasNoDanglingSurrogate() {
        String content = "字".repeat(79) + "😀";

        String summary = dispatchAndCaptureSummary(content);

        assertThat(Character.isHighSurrogate(summary.charAt(summary.length() - 1)))
                .isFalse();
        assertThat(content.startsWith(summary)).isTrue();
    }

    @Test
    @DisplayName("onCommentCreated_超长且边界不在代理对上_摘要长度恰好80")
    void onCommentCreated_boundaryNotOnSurrogatePair_summaryLengthIsExactlyLimit() {
        String content = "字".repeat(85);

        String summary = dispatchAndCaptureSummary(content);

        assertThat(summary).hasSize(80);
    }

    @Test
    @DisplayName("onCommentCreated_生成两个候选通知_共用评论幂等键")
    void onCommentCreated_twoCandidates_shareCommentDedupeKey() {
        Long commentId = 100L;
        CommentCreatedEvent event = new CommentCreatedEvent(commentId, ARTICLE_ID, 2L, 8L, "评论");

        listener.onCommentCreated(event);

        ArgumentCaptor<List<NotificationCreateDTO>> captor = ArgumentCaptor.forClass(List.class);
        verify(notificationPort).dispatch(captor.capture());
        assertThat(captor.getValue())
                .hasSize(2)
                .extracting(NotificationCreateDTO::dedupeKey)
                .containsOnly("comment:" + commentId);
    }
}
