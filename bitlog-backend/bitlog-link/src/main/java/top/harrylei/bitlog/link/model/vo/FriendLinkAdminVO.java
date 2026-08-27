package top.harrylei.bitlog.link.model.vo;

import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Data;
import lombok.experimental.Accessors;
import top.harrylei.bitlog.link.model.enums.FriendLinkStatusEnum;
import top.harrylei.bitlog.user.model.vo.UserVO;

import java.time.OffsetDateTime;

/**
 * 友链管理视图对象
 *
 * @author Harry
 * @since 2026-08-15
 */
@Data
@Accessors(chain = true)
@Schema(description = "友链管理视图对象")
public class FriendLinkAdminVO {

    @Schema(description = "友链 ID")
    private Long id;

    @Schema(description = "站点名称")
    private String name;

    @Schema(description = "站点地址")
    private String url;

    @Schema(description = "头像地址")
    private String avatar;

    @Schema(description = "站点简介")
    private String description;

    @Schema(description = "申请留言")
    private String applyMessage;

    @Schema(description = "状态：0-待审核，1-已通过，2-已拒绝")
    private FriendLinkStatusEnum status;

    @Schema(description = "拒绝理由")
    private String rejectReason;

    @Schema(description = "申请人，站长手动录入的友链为空")
    private UserVO applicant;

    @Schema(description = "提交时间")
    private OffsetDateTime createTime;

    @Schema(description = "最后更新时间")
    private OffsetDateTime updateTime;
}
