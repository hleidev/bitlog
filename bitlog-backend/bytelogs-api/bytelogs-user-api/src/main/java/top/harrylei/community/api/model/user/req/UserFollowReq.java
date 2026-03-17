package top.harrylei.community.api.model.user.req;

import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.NotNull;
import lombok.Data;

/**
 * 用户关注请求对象
 *
 * @author harry
 * @since 0.0.1
 */
@Data
@Schema(description = "用户关注请求")
public class UserFollowReq {

    @NotNull(message = "用户ID不能为空")
    @Schema(description = "要关注的用户ID", requiredMode = Schema.RequiredMode.REQUIRED, example = "123")
    private Long followeeId;
}
