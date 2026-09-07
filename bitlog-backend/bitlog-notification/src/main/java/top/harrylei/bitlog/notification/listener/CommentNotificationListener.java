package top.harrylei.bitlog.notification.listener;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import lombok.RequiredArgsConstructor;
import org.springframework.context.event.EventListener;
import org.springframework.stereotype.Component;
import top.harrylei.bitlog.article.port.ArticlePort;
import top.harrylei.bitlog.comment.event.CommentCreatedEvent;
import top.harrylei.bitlog.notification.model.dto.NotificationCreateDTO;
import top.harrylei.bitlog.notification.model.enums.NotificationTargetTypeEnum;
import top.harrylei.bitlog.notification.model.enums.NotificationTypeEnum;
import top.harrylei.bitlog.notification.port.NotificationPort;

/**
 * 评论创建后按规则列出候选通知并派发
 *
 * @author Harry
 * @since 2026-09-06
 */
@Component
@RequiredArgsConstructor
public class CommentNotificationListener {

    private static final int SUMMARY_MAX_LENGTH = 80;

    private final NotificationPort notificationPort;
    private final ArticlePort articlePort;

    /**
     * 同步监听评论创建事件，在发布方事务内落库候选通知
     *
     * @param event 评论创建事件
     */
    @EventListener
    public void onCommentCreated(CommentCreatedEvent event) {
        Map<String, Object> payload = buildPayload(event);
        List<NotificationCreateDTO> candidates = new ArrayList<>();

        if (event.repliedUserId() != null) {
            candidates.add(new NotificationCreateDTO(
                    event.repliedUserId(),
                    NotificationTypeEnum.COMMENT_REPLY,
                    event.authorId(),
                    NotificationTargetTypeEnum.COMMENT,
                    event.commentId(),
                    payload));
        }

        Long articleAuthorId = articlePort.getAuthorId(event.articleId());
        if (articleAuthorId != null) {
            candidates.add(new NotificationCreateDTO(
                    articleAuthorId,
                    NotificationTypeEnum.ARTICLE_COMMENT,
                    event.authorId(),
                    NotificationTargetTypeEnum.COMMENT,
                    event.commentId(),
                    payload));
        }

        if (!candidates.isEmpty()) {
            notificationPort.dispatch(candidates);
        }
    }

    /**
     * 构造渲染所需的快照
     *
     * @param event 评论创建事件
     * @return 快照 payload
     */
    private Map<String, Object> buildPayload(CommentCreatedEvent event) {
        Map<Long, String> titles = articlePort.getArticleTitles(List.of(event.articleId()));
        Map<String, Object> payload = new HashMap<>();
        payload.put("articleId", event.articleId());
        payload.put("articleTitle", titles.get(event.articleId()));
        payload.put("commentSummary", truncate(event.content()));
        return payload;
    }

    /**
     * 截断评论内容，避免切断代理对导致乱码
     *
     * @param content 原始内容
     * @return 截断后的摘要
     */
    private String truncate(String content) {
        if (content == null || content.length() <= SUMMARY_MAX_LENGTH) {
            return content;
        }
        int end = SUMMARY_MAX_LENGTH;
        if (Character.isHighSurrogate(content.charAt(end - 1))) {
            end--;
        }
        return content.substring(0, end);
    }
}
