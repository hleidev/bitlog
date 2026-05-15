package top.harrylei.bitlog.api.model.article.req;

import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.NotEmpty;
import lombok.Data;

import java.util.List;

/**
 * 标签批量删除请求
 *
 * @author harry
 * @since 0.0.1
 */
@Data
@Schema(description = "标签批量删除请求")
public class TagBatchDeleteRequest {

    @NotEmpty(message = "标签 ID 列表不能为空")
    @Schema(description = "标签 ID 列表", requiredMode = Schema.RequiredMode.REQUIRED)
    private List<Long> ids;
}
