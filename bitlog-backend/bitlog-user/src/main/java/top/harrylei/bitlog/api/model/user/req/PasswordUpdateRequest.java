package top.harrylei.bitlog.api.model.user.req;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Pattern;
import lombok.Data;

/**
 * 用户密码更新请求
 *
 * @author Harry
 * @since 2026-03-17
 */
@Data
public class PasswordUpdateRequest {

    @NotBlank(message = "旧密码不能为空")
    private String oldPassword;

    @NotBlank(message = "新密码不能为空")
    @Pattern(regexp = "^[a-zA-Z0-9_@#%&!$*-]{8,20}$", message = "新密码必须包含字母、数字，可包含特殊字符，长度为8~20位")
    private String newPassword;
}
