package top.harrylei.bitlog.api.model.auth;

import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Pattern;
import jakarta.validation.constraints.Size;
import top.harrylei.bitlog.api.model.user.UserRules;
import lombok.Data;
import top.harrylei.bitlog.api.enums.user.UserRoleEnum;

/**
 * 后台新建用户请求参数
 *
 * @author Harry
 * @since 2026-03-17
 */
@Data
@Schema(description = "后台新建用户请求参数")
public class UserCreateParam {

    @NotBlank(message = "邮箱不能为空")
    @Email(message = "邮箱格式不正确")
    @Size(max = 128, message = "邮箱长度不能超过 128 位")
    @Schema(description = "登录邮箱", example = "admin@example.com", requiredMode = Schema.RequiredMode.REQUIRED)
    private String email;

    @NotBlank(message = "用户名不能为空")
    @Pattern(regexp = UserRules.USERNAME_PATTERN, message = UserRules.USERNAME_MESSAGE)
    @Schema(description = "用户名", example = "admin", requiredMode = Schema.RequiredMode.REQUIRED)
    private String username;

    @NotBlank(message = "密码不能为空")
    @Pattern(regexp = "^[a-zA-Z0-9_@#%&!$*-]{8,20}$", message = "密码必须包含字母、数字，可包含特殊字符，长度为8~20位")
    @Schema(description = "初始密码", example = "admin@123", requiredMode = Schema.RequiredMode.REQUIRED)
    private String password;

    @NotNull(message = "角色不能为空")
    @Schema(description = "角色编码", example = "1", requiredMode = Schema.RequiredMode.REQUIRED)
    private UserRoleEnum role;
}
