package top.harrylei.bitlog.api.model.article.req;

import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.NotEmpty;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;
import lombok.Data;
import top.harrylei.bitlog.api.enums.article.ArticleStatusEnum;

import java.util.List;

/**
 * 文章批量状态更新请求
 *
 * @author harry
 * @since 0.0.1
 */
@Data
@Schema(description = "文章批量状态更新请求")
public class ArticleBatchStatusUpdateRequest {

    @NotEmpty(message = "文章 ID 列表不能为空")
    @Size(max = 100, message = "单次操作文章数不能超过 100")
    @Schema(description = "文章 ID 列表", requiredMode = Schema.RequiredMode.REQUIRED)
    private List<Long> ids;

    @NotNull(message = "目标状态不能为空")
    @Schema(description = "目标状态", requiredMode = Schema.RequiredMode.REQUIRED)
    private ArticleStatusEnum status;
}
