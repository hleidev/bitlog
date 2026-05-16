package top.harrylei.bitlog.api.model.article.vo;

import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Data;
import lombok.experimental.Accessors;

import java.time.LocalDateTime;

/**
 * 文章版本历史列表视图对象
 *
 * @author Harry
 * @since 2026-04-09
 */
@Data
@Accessors(chain = true)
@Schema(description = "文章版本历史列表视图对象")
public class ArticleVersionVO {

    @Schema(description = "版本 ID（article_version.id）")
    private Long id;

    @Schema(description = "文章 ID（article.id）")
    private Long articleId;

    @Schema(description = "版本号")
    private Integer version;

    @Schema(description = "该版本标题")
    private String title;

    @Schema(description = "版本创建时间")
    private LocalDateTime createTime;

    @Schema(description = "是否为最新草稿版本")
    private boolean latest;
}
