package top.harrylei.bitlog.api.model.comment.vo;

import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Data;
import lombok.experimental.Accessors;

import java.time.LocalDateTime;
import java.util.List;

/**
 * 根评论视图对象
 *
 * @author Harry
 * @since 2026-07-28
 */
@Data
@Accessors(chain = true)
@Schema(description = "根评论视图对象")
public class CommentVO {

    @Schema(description = "评论 ID")
    private Long id;

    @Schema(description = "评论内容，removed 为 true 时返回 null")
    private String content;

    @Schema(description = "评论者，removed 为 true 时返回 null")
    private CommentUserVO user;

    @Schema(description = "是否已被删除或隐藏，为 true 时前端渲染占位")
    private Boolean removed;

    @Schema(description = "回复列表")
    private List<CommentReplyVO> replies;

    @Schema(description = "该楼可见回复总数")
    private Integer replyCount;

    @Schema(description = "发表时间")
    private LocalDateTime createTime;
}
