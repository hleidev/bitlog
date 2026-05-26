package top.harrylei.bitlog.api.model.user.req;

import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.NotEmpty;
import lombok.Data;

import java.util.List;

/**
 * 用户 ID 列表请求参数
 *
 * @author Harry
 * @since 2026-04-10
 */
@Data
@Schema(description = "用户 ID 列表请求参数")
public class UserIdsParam {

    @NotEmpty(message = "用户 ID 列表不能为空")
    @Schema(description = "用户 ID 列表")
    private List<Long> userIds;
}
