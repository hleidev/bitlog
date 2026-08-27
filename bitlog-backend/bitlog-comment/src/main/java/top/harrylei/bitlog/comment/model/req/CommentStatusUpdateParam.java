package top.harrylei.bitlog.comment.model.req;

import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.NotNull;
import lombok.Data;
import top.harrylei.bitlog.comment.model.enums.CommentStatusEnum;

/**
 * 评论状态更新请求参数
 *
 * @author Harry
 * @since 2026-07-28
 */
@Data
@Schema(description = "评论状态更新请求参数")
public class CommentStatusUpdateParam {

    @NotNull(message = "评论状态不能为空")
    @Schema(description = "目标状态：1-正常，2-已隐藏", requiredMode = Schema.RequiredMode.REQUIRED)
    private CommentStatusEnum status;
}
