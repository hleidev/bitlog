package top.harrylei.bitlog.auth.model;

import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Pattern;
import jakarta.validation.constraints.Size;
import top.harrylei.bitlog.user.model.UserRules;
import lombok.Data;

/**
 * 注册请求参数
 *
 * @author Harry
 * @since 2026-07-30
 */
@Data
public class RegisterParam {

    @NotBlank(message = "邮箱不能为空")
    @Email(message = "邮箱格式不正确")
    @Size(max = 128, message = "邮箱长度不能超过 128 位")
    private String email;

    @NotBlank(message = "用户名不能为空")
    @Pattern(regexp = UserRules.USERNAME_PATTERN, message = UserRules.USERNAME_MESSAGE)
    private String username;

    @NotBlank(message = "密码不能为空")
    @Pattern(regexp = UserRules.PASSWORD_PATTERN, message = UserRules.PASSWORD_MESSAGE)
    private String password;

    @NotBlank(message = "验证码不能为空")
    @Pattern(regexp = "^\\d{6}$", message = "验证码为 6 位数字")
    private String code;
}
