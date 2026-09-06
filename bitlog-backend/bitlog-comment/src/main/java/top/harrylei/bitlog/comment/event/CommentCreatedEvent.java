package top.harrylei.bitlog.comment.event;

/**
 * 评论已创建
 *
 * @param commentId 评论 ID
 * @param articleId 文章 ID
 * @param authorId 评论者，作为通知的触发者
 * @param repliedUserId 被回复者，无人被回复时为 null
 * @param content 评论内容
 * @author Harry
 * @since 2026-09-06
 */
public record CommentCreatedEvent(Long commentId, Long articleId, Long authorId, Long repliedUserId, String content) {
}
