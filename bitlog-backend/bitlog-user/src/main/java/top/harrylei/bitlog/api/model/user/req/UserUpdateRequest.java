package top.harrylei.bitlog.api.model.user.req;

import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;
import lombok.Data;
import lombok.experimental.Accessors;

/**
 * 用户信息更新请求
 *
 * @author Harry
 * @since 2026-03-17
 */
@Data
@Schema(description = "用户信息更新请求")
@Accessors(chain = true)
public class UserUpdateRequest {

    @NotBlank(message = "昵称不能为空")
    @Size(max = 64, message = "昵称最长64字符")
    @Schema(description = "昵称", requiredMode = Schema.RequiredMode.REQUIRED)
    private String nickname;

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
