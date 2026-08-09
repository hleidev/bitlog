package top.harrylei.bitlog.api.model.auth;

import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Data;
import lombok.experimental.Accessors;

/**
 * 管理员创建用户结果展示对象
 *
 * @author Harry
 * @since 2026-04-10
 */
@Data
@Accessors(chain = true)
@Schema(description = "管理员创建用户结果展示对象")
public class UserCreatedVO {

    /** 必须回传：登录标识是邮箱，只给用户名会让管理员把一组登不进去的凭据转告新用户 */
    @Schema(description = "登录邮箱")
    private String email;

    @Schema(description = "用户名，公开展示")
    private String username;

    @Schema(description = "系统生成的初始密码（明文）")
    private String initialPassword;
}
