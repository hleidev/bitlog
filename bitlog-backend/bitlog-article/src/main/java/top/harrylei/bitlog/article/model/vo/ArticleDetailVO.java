package top.harrylei.bitlog.article.model.vo;

import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Data;
import lombok.EqualsAndHashCode;
import lombok.experimental.Accessors;

/**
 * 文章详情视图对象（含正文内容及版本信息）
 *
 * @author Harry
 * @since 2026-04-09
 */
@Data
@EqualsAndHashCode(callSuper = true)
@Accessors(chain = true)
@Schema(description = "文章详情视图对象")
public class ArticleDetailVO extends ArticleVO {

    @Schema(description = "文章正文内容")
    private String content;

    @Schema(description = "当前版本 ID（article_version.id）")
    private Long versionId;

    @Schema(description = "当前版本号")
    private Integer version;
}
