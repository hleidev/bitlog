package top.harrylei.bitlog.api.model.article.req;

import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.NotEmpty;
import lombok.Data;

import java.util.List;

/**
 * 文章批量删除请求
 *
 * @author harry
 * @since 0.0.1
 */
@Data
@Schema(description = "文章批量删除请求")
public class ArticleBatchDeleteRequest {

    @NotEmpty(message = "文章 ID 列表不能为空")
    @Schema(description = "文章 ID 列表", requiredMode = Schema.RequiredMode.REQUIRED)
    private List<Long> ids;
}
