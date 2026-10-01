package top.harrylei.bitlog.article.model.req;

import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;
import lombok.Data;
import lombok.experimental.Accessors;

/**
 * AI 元数据推荐请求参数（编辑器当前内容，无需先保存）
 *
 * @author Harry
 * @since 2026-10-01
 */
@Data
@Accessors(chain = true)
@Schema(description = "AI 元数据推荐请求参数")
public class ArticleAiMetadataParam {

    @NotBlank(message = "标题不能为空")
    @Size(max = 200, message = "标题长度不能超过 200 个字符")
    @Schema(description = "文章标题", requiredMode = Schema.RequiredMode.REQUIRED)
    private String title;

    @NotBlank(message = "内容不能为空")
    @Schema(description = "文章正文内容", requiredMode = Schema.RequiredMode.REQUIRED)
    private String content;
}
