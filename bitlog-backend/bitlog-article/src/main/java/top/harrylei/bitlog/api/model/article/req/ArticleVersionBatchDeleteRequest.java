package top.harrylei.bitlog.api.model.article.req;

import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.NotEmpty;
import jakarta.validation.constraints.Size;
import lombok.Data;

import java.util.List;

/**
 * 文章版本批量删除请求
 *
 * @author Harry
 * @since 2026-05-24
 */
@Data
@Schema(description = "文章版本批量删除请求")
public class ArticleVersionBatchDeleteRequest {

    @NotEmpty(message = "版本 ID 列表不能为空")
    @Size(max = 50, message = "单次操作版本数不能超过 50")
    @Schema(description = "版本 ID 列表", requiredMode = Schema.RequiredMode.REQUIRED)
    private List<Long> versionIds;
}
