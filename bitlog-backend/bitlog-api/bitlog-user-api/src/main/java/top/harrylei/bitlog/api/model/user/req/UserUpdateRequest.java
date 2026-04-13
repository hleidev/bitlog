package top.harrylei.bitlog.api.model.user.req;

import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Pattern;
import jakarta.validation.constraints.Size;
import lombok.Data;
import lombok.experimental.Accessors;

/**
 * 用户信息更新请求
 *
 * @author harry
 * @since 0.0.1
 */
@Data
@Schema(description = "用户信息更新请求")
@Accessors(chain = true)
public class UserUpdateRequest {

    @NotBlank(message = "用户名不能为空")
    @Pattern(regexp = "^[a-zA-Z0-9_-]{4,16}$", message = "用户名只能包含字母、数字、下划线和连字符，长度为4-16位")
    @Schema(description = "用户名", requiredMode = Schema.RequiredMode.REQUIRED)
    private String userName;

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
