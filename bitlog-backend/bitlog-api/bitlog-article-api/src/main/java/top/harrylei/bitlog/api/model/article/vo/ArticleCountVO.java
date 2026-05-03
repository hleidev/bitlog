package top.harrylei.bitlog.api.model.article.vo;

import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Data;
import lombok.experimental.Accessors;

/**
 * 文章各状态数量统计
 *
 * @author harry
 * @since 0.0.1
 */
@Data
@Accessors(chain = true)
@Schema(description = "文章各状态数量统计")
public class ArticleCountVO {

    @Schema(description = "全部文章数")
    private long total;

    @Schema(description = "已发布文章数")
    private long published;

    @Schema(description = "草稿文章数")
    private long draft;
}
