package top.harrylei.bitlog.link.model.req;

import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;
import lombok.Data;
import top.harrylei.bitlog.link.model.enums.FriendLinkStatusEnum;

/**
 * 友链审核请求参数
 *
 * @author Harry
 * @since 2026-08-15
 */
@Data
@Schema(description = "友链审核请求参数")
public class FriendLinkAuditParam {

    @NotNull(message = "审核结果不能为空")
    @Schema(description = "审核结果：1-通过，2-拒绝", requiredMode = Schema.RequiredMode.REQUIRED)
    private FriendLinkStatusEnum status;

    @Size(max = 255, message = "拒绝理由不能超过 255 个字符")
    @Schema(description = "拒绝理由，申请人可见；通过时忽略")
    private String rejectReason;
}
