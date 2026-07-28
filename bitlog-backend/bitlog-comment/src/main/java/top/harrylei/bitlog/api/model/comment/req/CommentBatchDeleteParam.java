package top.harrylei.bitlog.api.model.comment.req;

import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.NotEmpty;
import jakarta.validation.constraints.Size;
import lombok.Data;

import java.util.List;

/**
 * 评论批量删除请求参数
 *
 * @author Harry
 * @since 2026-07-28
 */
@Data
@Schema(description = "评论批量删除请求参数")
public class CommentBatchDeleteParam {

    @NotEmpty(message = "评论 ID 列表不能为空")
    @Size(max = 100, message = "单次操作评论数不能超过 100")
    @Schema(description = "评论 ID 列表", requiredMode = Schema.RequiredMode.REQUIRED)
    private List<Long> ids;
}
