package top.harrylei.bitlog.api.model.article.req;

import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.NotEmpty;
import jakarta.validation.constraints.Size;
import lombok.Data;

import java.util.List;

/**
 * 文章批量删除请求参数
 *
 * @author Harry
 * @since 2026-04-15
 */
@Data
@Schema(description = "文章批量删除请求参数")
public class ArticleBatchDeleteParam {

    @NotEmpty(message = "文章 ID 列表不能为空")
    @Size(max = 100, message = "单次操作文章数不能超过 100")
    @Schema(description = "文章 ID 列表", requiredMode = Schema.RequiredMode.REQUIRED)
    private List<Long> ids;
}
