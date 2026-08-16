package top.harrylei.bitlog.api.model.article.dto;

import lombok.Data;

/**
 * 我的文章各状态数量统计传输对象
 *
 * @author Harry
 * @since 2026-08-16
 */
@Data
public class ArticleCountDTO {

    private long total;

    private long published;

    private long draft;
}
