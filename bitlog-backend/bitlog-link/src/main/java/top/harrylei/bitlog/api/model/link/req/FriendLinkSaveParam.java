package top.harrylei.bitlog.api.model.link.req;

import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Pattern;
import jakarta.validation.constraints.Size;
import lombok.Data;

/**
 * 友链提交请求参数，申请与修改共用
 *
 * @author Harry
 * @since 2026-08-15
 */
@Data
@Schema(description = "友链提交请求参数")
public class FriendLinkSaveParam {

    @NotBlank(message = "站点名称不能为空")
    @Size(max = 64, message = "站点名称不能超过 64 个字符")
    @Schema(description = "站点名称", requiredMode = Schema.RequiredMode.REQUIRED)
    private String name;

    @NotBlank(message = "站点地址不能为空")
    @Size(max = 512, message = "站点地址不能超过 512 个字符")
    @Pattern(regexp = "^https?://.+", message = "站点地址必须以 http:// 或 https:// 开头")
    @Schema(description = "站点地址", requiredMode = Schema.RequiredMode.REQUIRED)
    private String url;

    @Size(max = 512, message = "头像地址不能超过 512 个字符")
    @Pattern(regexp = "^$|^https?://.+", message = "头像地址必须以 http:// 或 https:// 开头")
    @Schema(description = "头像地址")
    private String avatar;

    @Size(max = 255, message = "简介不能超过 255 个字符")
    @Schema(description = "站点简介")
    private String description;

    @Size(max = 500, message = "留言不能超过 500 个字符")
    @Schema(description = "申请留言")
    private String applyMessage;
}
