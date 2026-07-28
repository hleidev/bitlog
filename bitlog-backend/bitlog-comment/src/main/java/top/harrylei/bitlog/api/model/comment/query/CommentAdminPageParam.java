package top.harrylei.bitlog.api.model.comment.query;

import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.Size;
import lombok.Data;
import lombok.EqualsAndHashCode;
import lombok.experimental.Accessors;
import top.harrylei.bitlog.api.enums.comment.CommentStatusEnum;
import top.harrylei.bitlog.common.model.BasePage;

/**
 * 评论管理列表查询参数
 *
 * @author Harry
 * @since 2026-07-28
 */
@Data
@EqualsAndHashCode(callSuper = true)
@Accessors(chain = true)
@Schema(description = "评论管理列表查询参数")
public class CommentAdminPageParam extends BasePage {

    @Schema(description = "文章 ID")
    private Long articleId;

    @Schema(description = "评论者用户 ID")
    private Long userId;

    @Schema(description = "评论状态：1-正常，2-已隐藏")
    private CommentStatusEnum status;

    @Size(max = 50, message = "关键词长度不能超过 50")
    @Schema(description = "关键词（搜索评论内容）")
    private String keyword;
}
