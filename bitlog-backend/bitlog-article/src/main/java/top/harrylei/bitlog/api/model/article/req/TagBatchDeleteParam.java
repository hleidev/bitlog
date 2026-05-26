package top.harrylei.bitlog.api.model.article.req;

import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.NotEmpty;
import lombok.Data;

import java.util.List;

/**
 * 标签批量删除请求参数
 *
 * @author Harry
 * @since 2026-04-15
 */
@Data
@Schema(description = "标签批量删除请求参数")
public class TagBatchDeleteParam {

    @NotEmpty(message = "标签 ID 列表不能为空")
    @Schema(description = "标签 ID 列表", requiredMode = Schema.RequiredMode.REQUIRED)
    private List<Long> ids;
}
