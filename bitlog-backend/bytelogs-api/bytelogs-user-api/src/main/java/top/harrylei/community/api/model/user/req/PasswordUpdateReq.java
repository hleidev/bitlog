package top.harrylei.community.api.model.user.req;

import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Pattern;
import lombok.Data;

/**
 * 用户密码更新请求
 *
 * @author harry
 * @since 0.0.1
 */
@Data
@Schema(description = "用户密码更新请求")
public class PasswordUpdateReq {

    @NotBlank(message = "旧密码不能为空")
    @Schema(description = "旧密码", requiredMode = Schema.RequiredMode.REQUIRED)
    private String oldPassword;

    @NotBlank(message = "新密码不能为空")
    @Pattern(regexp = "^[a-zA-Z0-9_@#%&!$*-]{8,20}$", message = "新密码必须包含字母、数字，可包含特殊字符，长度为8~20位")
    @Schema(description = "新密码", requiredMode = Schema.RequiredMode.REQUIRED)
    private String newPassword;
}
