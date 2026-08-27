package top.harrylei.bitlog.user.model.req;

import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Pattern;
import jakarta.validation.constraints.Size;
import lombok.Data;
import lombok.experimental.Accessors;
import top.harrylei.bitlog.user.model.UserRules;

/**
 * 用户信息更新请求参数
 *
 * @author Harry
 * @since 2026-03-17
 */
@Data
@Schema(description = "用户信息更新请求参数")
@Accessors(chain = true)
public class UserUpdateParam {

    @NotBlank(message = "用户名不能为空")
    @Pattern(regexp = UserRules.USERNAME_PATTERN, message = UserRules.USERNAME_MESSAGE)
    @Schema(description = "用户名", requiredMode = Schema.RequiredMode.REQUIRED)
    private String username;

    @Size(max = 500, message = "个人简介最长500字符")
    @Schema(description = "用户简介")
    private String profile;

    @Size(max = 64, message = "职位最长64字符")
    @Schema(description = "个人职位")
    private String position;

    @Size(max = 64, message = "公司最长64字符")
    @Schema(description = "公司")
    private String company;
}
