package top.harrylei.bitlog.api.model.comment.vo;

import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Data;
import lombok.experimental.Accessors;
import top.harrylei.bitlog.api.enums.comment.CommentStatusEnum;

import java.time.LocalDateTime;

/**
 * 评论管理视图对象
 *
 * @author Harry
 * @since 2026-07-28
 */
@Data
@Accessors(chain = true)
@Schema(description = "评论管理视图对象")
public class CommentAdminVO {

    @Schema(description = "评论 ID")
    private Long id;

    @Schema(description = "所属文章 ID")
    private Long articleId;

    @Schema(description = "所属文章标题")
    private String articleTitle;

    @Schema(description = "评论内容")
    private String content;

    @Schema(description = "评论者")
    private CommentUserVO user;

    @Schema(description = "根评论 ID，0 表示自身即根评论")
    private Long rootId;

    @Schema(description = "评论状态")
    private CommentStatusEnum status;

    @Schema(description = "发表时间")
    private LocalDateTime createTime;
}
