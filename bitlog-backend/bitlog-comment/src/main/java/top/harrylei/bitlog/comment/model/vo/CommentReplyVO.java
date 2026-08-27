package top.harrylei.bitlog.comment.model.vo;

import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Data;
import lombok.experimental.Accessors;

import java.time.OffsetDateTime;

/**
 * 评论回复视图对象
 *
 * @author Harry
 * @since 2026-07-28
 */
@Data
@Accessors(chain = true)
@Schema(description = "评论回复视图对象")
public class CommentReplyVO {

    @Schema(description = "评论 ID")
    private Long id;

    @Schema(description = "评论内容")
    private String content;

    @Schema(description = "评论者")
    private CommentUserVO user;

    @Schema(description = "被回复者，直接回复根评论时返回 null")
    private CommentUserVO replyToUser;

    @Schema(description = "发表时间")
    private OffsetDateTime createTime;
}
