package top.harrylei.bitlog.auth.model;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Pattern;
import top.harrylei.bitlog.user.model.UserRules;
import lombok.Data;

/**
 * 用户密码更新请求参数
 *
 * @author Harry
 * @since 2026-03-17
 */
@Data
public class PasswordUpdateParam {

    @NotBlank(message = "旧密码不能为空")
    private String oldPassword;

    @NotBlank(message = "新密码不能为空")
    @Pattern(regexp = UserRules.PASSWORD_PATTERN, message = UserRules.PASSWORD_MESSAGE)
    private String newPassword;
}
