package top.harrylei.bitlog.api.model.article.req;

import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.Size;
import lombok.Data;
import lombok.experimental.Accessors;

import java.util.List;

/**
 * 文章发布请求（封面、摘要、分类、标签）
 *
 * @author harry
 * @since 0.0.1
 */
@Data
@Accessors(chain = true)
@Schema(description = "文章发布请求")
public class ArticlePublishRequest {

    @Size(max = 512, message = "封面图地址长度不能超过 512 个字符")
    @Schema(description = "封面图地址")
    private String cover;

    @Size(max = 512, message = "摘要长度不能超过 512 个字符")
    @Schema(description = "文章摘要")
    private String summary;

    @Schema(description = "分类 ID")
    private Long categoryId;

    @Schema(description = "标签 ID 列表")
    private List<Long> tagIds;
}
