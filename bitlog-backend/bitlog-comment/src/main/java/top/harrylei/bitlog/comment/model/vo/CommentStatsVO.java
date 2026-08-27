package top.harrylei.bitlog.comment.model.vo;

import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Data;
import lombok.experimental.Accessors;

/**
 * 评论数量统计展示对象
 *
 * @author Harry
 * @since 2026-08-16
 */
@Data
@Accessors(chain = true)
@Schema(description = "评论数量统计展示对象")
public class CommentStatsVO {

    @Schema(description = "评论总数（不含已删除）")
    private long total;

    @Schema(description = "显示中的评论数")
    private long visible;

    @Schema(description = "已隐藏的评论数")
    private long hidden;
}
