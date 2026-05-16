package top.harrylei.bitlog.api.model.user.req;

import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.NotEmpty;
import jakarta.validation.constraints.NotNull;
import lombok.Data;
import top.harrylei.bitlog.api.enums.user.UserStatusEnum;

import java.util.List;

/**
 * 用户批量修改状态请求
 *
 * @author Harry
 * @since 2026-04-10
 */
@Data
@Schema(description = "用户批量修改状态请求")
public class UserStatusUpdateRequest {

    @NotEmpty(message = "用户 ID 列表不能为空")
    @Schema(description = "用户 ID 列表")
    private List<Long> userIds;

    @NotNull(message = "目标状态不能为空")
    @Schema(description = "目标状态：0-禁用，1-启用")
    private UserStatusEnum status;
}
