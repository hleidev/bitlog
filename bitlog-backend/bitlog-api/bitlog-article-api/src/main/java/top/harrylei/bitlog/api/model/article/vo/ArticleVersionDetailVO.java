package top.harrylei.bitlog.api.model.article.vo;

import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Data;
import lombok.experimental.Accessors;

import java.time.LocalDateTime;

/**
 * 文章版本详情视图对象（含正文内容，用于版本对比）
 *
 * @author harry
 * @since 0.0.1
 */
@Data
@Accessors(chain = true)
@Schema(description = "文章版本详情视图对象")
public class ArticleVersionDetailVO {

    @Schema(description = "版本 ID（article_version.id）")
    private Long id;

    @Schema(description = "文章 ID（article.id）")
    private Long articleId;

    @Schema(description = "版本号")
    private Integer version;

    @Schema(description = "该版本标题")
    private String title;

    @Schema(description = "该版本正文内容")
    private String content;

    @Schema(description = "版本创建时间")
    private LocalDateTime createTime;
}
