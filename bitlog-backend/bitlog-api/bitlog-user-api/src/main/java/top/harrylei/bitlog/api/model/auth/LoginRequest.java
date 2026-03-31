package top.harrylei.bitlog.api.model.auth;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Pattern;
import lombok.Data;

/**
 * 认证请求
 *
 * @author harry
 * @since 0.0.1
 */
@Data
public class LoginRequest {

    @NotBlank(message = "用户名不能为空")
    @Pattern(regexp = "^[a-zA-Z0-9_-]{4,16}$", message = "用户名只能包含字母、数字、下划线和连字符，长度为4-16位")
    private String username;

    @NotBlank(message = "密码不能为空")
    @Pattern(regexp = "^[a-zA-Z0-9_@#%&!$*-]{8,20}$", message = "密码必须包含字母、数字，可包含特殊字符，长度为8~20位")
    private String password;

    private Boolean keepLogin = false;
}
