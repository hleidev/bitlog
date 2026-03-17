package top.harrylei.community.api.model.auth;

import io.swagger.v3.oas.annotations.media.Schema;
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
@Schema(description = "认证请求")
public class AuthReq {

    @NotBlank(message = "用户名不能为空")
    @Pattern(regexp = "^[a-zA-Z0-9_-]{4,16}$", message = "用户名只能包含字母、数字、下划线和连字符，长度为4-16位")
    @Schema(description = "用户名", example = "user", requiredMode = Schema.RequiredMode.REQUIRED)
    private String username;

    @NotBlank(message = "密码不能为空")
    @Pattern(regexp = "^[a-zA-Z0-9_@#%&!$*-]{8,20}$", message = "密码必须包含字母、数字，可包含特殊字符，长度为8~20位")
    @Schema(description = "密码", example = "user@123", requiredMode = Schema.RequiredMode.REQUIRED)
    private String password;

    @Schema(description = "保持登录状态", example = "false", requiredMode = Schema.RequiredMode.NOT_REQUIRED)
    private Boolean keepLogin = false;
}
