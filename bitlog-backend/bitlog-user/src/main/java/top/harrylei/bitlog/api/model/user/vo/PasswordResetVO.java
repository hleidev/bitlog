package top.harrylei.bitlog.api.model.user.vo;

import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Data;
import lombok.experimental.Accessors;

/**
 * 重置密码结果展示对象
 *
 * @author harry
 * @since 0.0.1
 */
@Data
@Accessors(chain = true)
@Schema(description = "重置密码结果展示对象")
public class PasswordResetVO {

    @Schema(description = "重置后的新密码（明文）")
    private String newPassword;
}
