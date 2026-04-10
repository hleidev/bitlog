package top.harrylei.bitlog.api.model.user.req;

import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.NotEmpty;
import lombok.Data;

import java.util.List;

/**
 * 用户 ID 列表请求
 *
 * @author harry
 * @since 0.0.1
 */
@Data
@Schema(description = "用户 ID 列表请求")
public class UserIdsRequest {

    @NotEmpty(message = "用户 ID 列表不能为空")
    @Schema(description = "用户 ID 列表")
    private List<Long> userIds;
}
