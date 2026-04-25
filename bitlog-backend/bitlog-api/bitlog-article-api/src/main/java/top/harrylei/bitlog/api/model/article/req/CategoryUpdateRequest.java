package top.harrylei.bitlog.api.model.article.req;

import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;
import lombok.Data;

/**
 * 分类更新请求
 *
 * @author harry
 * @since 0.0.1
 */
@Data
@Schema(description = "分类更新请求")
public class CategoryUpdateRequest {
    @NotBlank(message = "分类名称不能为空")
    @Size(max = 64, message = "分类名称不能超过 64 个字符")
    @Schema(description = "分类名称", requiredMode = Schema.RequiredMode.REQUIRED)
    private String name;

    @Schema(description = "父分类 ID（不传=不改归属；0=升为顶级；正数=移到指定父分类）",
            requiredMode = Schema.RequiredMode.NOT_REQUIRED)
    private Long parentId;
}
