package top.harrylei.bitlog.api.model.article.vo;

import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Data;
import lombok.EqualsAndHashCode;
import lombok.experimental.Accessors;

import java.util.List;

/**
 * 文章详情视图对象（含正文内容及版本信息）
 *
 * @author harry
 * @since 0.0.1
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

    @Schema(description = "标签列表")
    private List<String> tags;
}
