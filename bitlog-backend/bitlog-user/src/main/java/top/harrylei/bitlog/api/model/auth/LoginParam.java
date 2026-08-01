package top.harrylei.bitlog.api.model.auth;

import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;
import top.harrylei.bitlog.api.model.user.UserRules;
import lombok.Data;

/**
 * 认证请求参数
 *
 * @author Harry
 * @since 2026-03-17
 */
@Data
public class LoginParam {

    @NotBlank(message = "邮箱不能为空")
    @Email(message = "邮箱格式不正确")
    @Size(max = 128, message = "邮箱长度不能超过 128 位")
    private String email;

    /**
     * 登录不校验密码格式，只挡空值与超长输入。 格式规则属于注册期约束，用在这里会让规则一旦调整，存量用户即使输对密码也被判「格式不正确」。 正确性交给 BCrypt 比对。
     */
    @NotBlank(message = "密码不能为空")
    @Size(max = UserRules.PASSWORD_MAX_INPUT, message = "密码长度不能超过 128 位")
    private String password;
}
