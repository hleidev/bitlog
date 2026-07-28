package top.harrylei.bitlog.api.model.comment.req;

import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;
import lombok.Data;

/**
 * 评论发表请求参数
 *
 * @author Harry
 * @since 2026-07-28
 */
@Data
@Schema(description = "评论发表请求参数")
public class CommentSaveParam {

    @NotBlank(message = "评论内容不能为空")
    @Size(max = 1000, message = "评论内容不能超过 1000 个字符")
    @Schema(description = "评论内容（纯文本）", requiredMode = Schema.RequiredMode.REQUIRED)
    private String content;

    @Schema(description = "父评论 ID，为空或 0 表示发表一级评论")
    private Long parentId;
}
