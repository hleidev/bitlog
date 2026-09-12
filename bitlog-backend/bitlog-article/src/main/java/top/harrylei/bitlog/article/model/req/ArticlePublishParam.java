package top.harrylei.bitlog.article.model.req;

import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;
import java.util.List;
import lombok.Data;
import lombok.experimental.Accessors;

/**
 * 文章发布请求参数（确认的版本、摘要、分类、标签）
 *
 * @author Harry
 * @since 2026-04-09
 */
@Data
@Accessors(chain = true)
@Schema(description = "文章发布请求参数")
public class ArticlePublishParam {

    @NotNull(message = "发布版本不能为空")
    @Schema(description = "用户确认发布的草稿版本 ID", requiredMode = Schema.RequiredMode.REQUIRED)
    private Long expectedVersionId;

    @Size(max = 512, message = "摘要长度不能超过 512 个字符")
    @Schema(description = "文章摘要")
    private String summary;

    @NotNull(message = "分类不能为空")
    @Schema(description = "分类 ID", requiredMode = Schema.RequiredMode.REQUIRED)
    private Long categoryId;

    @Schema(description = "标签 ID 列表")
    private List<Long> tagIds;
}
