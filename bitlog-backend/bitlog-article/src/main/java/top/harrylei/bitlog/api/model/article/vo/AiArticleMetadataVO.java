package top.harrylei.bitlog.api.model.article.vo;

import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Data;
import lombok.experimental.Accessors;

import java.util.List;

/**
 * AI 文章元数据推荐视图对象
 *
 * @author Harry
 * @since 2026-05-25
 */
@Data
@Accessors(chain = true)
@Schema(description = "AI 文章元数据推荐")
public class AiArticleMetadataVO {

    @Schema(description = "AI 生成的摘要")
    private String summary;

    @Schema(description = "推荐分类，null 表示现有分类中无合适选项")
    private CategoryVO category;

    @Schema(description = "从现有标签中匹配的推荐标签")
    private List<TagVO> tags;

    @Schema(description = "现有标签覆盖不到时建议新建的标签名称")
    private List<String> suggestedTags;
}
