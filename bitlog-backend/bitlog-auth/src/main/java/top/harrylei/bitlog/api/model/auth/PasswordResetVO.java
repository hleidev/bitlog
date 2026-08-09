package top.harrylei.bitlog.api.model.auth;

import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Data;
import lombok.experimental.Accessors;

/**
 * 重置密码结果展示对象
 *
 * @author Harry
 * @since 2026-04-10
 */
@Data
@Accessors(chain = true)
@Schema(description = "重置密码结果展示对象")
public class PasswordResetVO {

    @Schema(description = "重置后的新密码（明文）")
    private String newPassword;
}
