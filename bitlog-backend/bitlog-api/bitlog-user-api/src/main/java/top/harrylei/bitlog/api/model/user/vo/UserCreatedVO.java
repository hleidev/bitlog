package top.harrylei.bitlog.api.model.user.vo;

import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Data;
import lombok.experimental.Accessors;

/**
 * 管理员创建用户结果展示对象
 *
 * @author harry
 * @since 0.0.1
 */
@Data
@Accessors(chain = true)
@Schema(description = "管理员创建用户结果展示对象")
public class UserCreatedVO {

    @Schema(description = "用户名")
    private String userName;

    @Schema(description = "系统生成的初始密码（明文）")
    private String initialPassword;
}
