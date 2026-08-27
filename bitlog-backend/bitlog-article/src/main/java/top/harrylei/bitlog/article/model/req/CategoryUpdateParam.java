package top.harrylei.bitlog.article.model.req;

import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;
import lombok.Data;

/**
 * 分类更新请求参数
 *
 * @author Harry
 * @since 2026-04-09
 */
@Data
@Schema(description = "分类更新请求参数")
public class CategoryUpdateParam {
    @NotBlank(message = "分类名称不能为空")
    @Size(max = 64, message = "分类名称不能超过 64 个字符")
    @Schema(description = "分类名称", requiredMode = Schema.RequiredMode.REQUIRED)
    private String name;

}
