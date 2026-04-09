package top.harrylei.bitlog.api.model.article.req;

import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;
import lombok.Data;

/**
 * 分类创建/更新请求
 *
 * @author harry
 * @since 0.0.1
 */
@Data
@Schema(description = "分类创建/更新请求")
public class CategorySaveRequest {
    @NotBlank(message = "分类名称不能为空")
    @Size(max = 64, message = "分类名称不能超过 64 个字符")
    @Schema(description = "分类名称", requiredMode = Schema.RequiredMode.REQUIRED)
    private String name;

    @Size(max = 256, message = "分类描述不能超过 256 个字符")
    @Schema(description = "分类描述")
    private String description;

    @Schema(description = "排序权重，越大越靠前，默认 0")
    private Integer sortOrder = 0;
}
