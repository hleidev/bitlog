package top.harrylei.bitlog.api.model.comment.vo;

import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Data;
import lombok.experimental.Accessors;

/**
 * 评论用户视图对象
 *
 * @author Harry
 * @since 2026-07-28
 */
@Data
@Accessors(chain = true)
@Schema(description = "评论用户视图对象")
public class CommentUserVO {

    @Schema(description = "用户 ID")
    private Long userId;

    @Schema(description = "昵称")
    private String nickname;

    @Schema(description = "头像 URL")
    private String avatar;
}
