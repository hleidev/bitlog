package top.harrylei.bitlog.api.model.user.req;

import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Pattern;
import lombok.Data;
import lombok.experimental.Accessors;
import top.harrylei.bitlog.api.enums.user.UserRoleEnum;

/**
 * 管理员创建用户请求
 *
 * @author harry
 * @since 0.0.1
 */
@Data
@Accessors(chain = true)
@Schema(description = "管理员创建用户请求")
public class AdminCreateUserRequest {

    @NotBlank(message = "用户名不能为空")
    @Pattern(regexp = "^[a-zA-Z0-9_-]{4,16}$", message = "用户名只能包含字母、数字、下划线和连字符，长度为4-16位")
    @Schema(description = "用户名", requiredMode = Schema.RequiredMode.REQUIRED)
    private String userName;

    @Email(message = "邮箱格式不正确")
    @Schema(description = "邮箱（选填）")
    private String email;

    @NotNull(message = "用户角色不能为空")
    @Schema(description = "用户角色", requiredMode = Schema.RequiredMode.REQUIRED)
    private UserRoleEnum userRole;

    @Schema(description = "职位")
    private String position;

    @Schema(description = "公司")
    private String company;

    @Schema(description = "个人简介")
    private String profile;
}
