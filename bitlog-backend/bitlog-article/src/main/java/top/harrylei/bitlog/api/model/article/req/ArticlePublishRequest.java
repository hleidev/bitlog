package top.harrylei.bitlog.api.model.article.req;

import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.Size;
import lombok.Data;
import lombok.experimental.Accessors;

import java.util.List;

/**
 * 文章发布请求（封面、摘要、分类、标签）
 *
 * @author Harry
 * @since 2026-04-09
 */
@Data
@Accessors(chain = true)
@Schema(description = "文章发布请求")
public class ArticlePublishRequest {

    @Size(max = 512, message = "摘要长度不能超过 512 个字符")
    @Schema(description = "文章摘要")
    private String summary;

    @Schema(description = "分类 ID")
    private Long categoryId;

    @Schema(description = "标签 ID 列表")
    private List<Long> tagIds;
}
