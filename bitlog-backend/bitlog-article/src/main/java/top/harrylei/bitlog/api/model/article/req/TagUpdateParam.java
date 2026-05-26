package top.harrylei.bitlog.api.model.article.req;

import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;
import lombok.Data;

/**
 * 标签更新请求参数
 *
 * @author Harry
 * @since 2026-04-02
 */
@Data
@Schema(description = "标签更新请求参数")
public class TagUpdateParam {

    @NotBlank(message = "标签名称不能为空")
    @Size(max = 64, message = "标签名称不能超过 64 个字符")
    @Schema(description = "标签名称", requiredMode = Schema.RequiredMode.REQUIRED)
    private String name;
}
