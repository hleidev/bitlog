package top.harrylei.bitlog.comment.model.dto;

import lombok.Data;

/**
 * 评论数量统计传输对象
 *
 * @author Harry
 * @since 2026-08-16
 */
@Data
public class CommentStatsDTO {

    private long total;

    private long visible;

    private long hidden;
}
