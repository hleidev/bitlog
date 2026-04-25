package top.harrylei.bitlog.api.model.article.req;

import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;
import lombok.Data;

/**
 * 分类创建请求
 *
 * @author harry
 * @since 0.0.1
 */
@Data
@Schema(description = "分类创建请求")
public class CategoryCreateRequest {
    @NotBlank(message = "分类名称不能为空")
    @Size(max = 64, message = "分类名称不能超过 64 个字符")
    @Schema(description = "分类名称", requiredMode = Schema.RequiredMode.REQUIRED)
    private String name;

    @Schema(description = "父分类 ID，不传或传 0 表示顶级分类")
    private Long parentId;
}
