package top.harrylei.bitlog.auth.model;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Pattern;
import top.harrylei.bitlog.user.model.UserRules;
import lombok.Data;

/**
 * 首次设置密码请求参数，供第三方登录建号的无密码账号使用
 *
 * @author Harry
 * @since 2026-08-02
 */
@Data
public class PasswordInitParam {

    @NotBlank(message = "密码不能为空")
    @Pattern(regexp = UserRules.PASSWORD_PATTERN, message = UserRules.PASSWORD_MESSAGE)
    private String password;
}
