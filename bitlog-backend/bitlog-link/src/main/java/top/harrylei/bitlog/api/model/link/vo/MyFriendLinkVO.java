package top.harrylei.bitlog.api.model.link.vo;

import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Data;
import lombok.experimental.Accessors;
import top.harrylei.bitlog.api.enums.link.FriendLinkStatusEnum;

import java.time.OffsetDateTime;

/**
 * 我的友链视图对象
 * <p>
 * 只返回给申请人本人，比公开视图多出状态与拒绝理由。
 * </p>
 *
 * @author Harry
 * @since 2026-08-15
 */
@Data
@Accessors(chain = true)
@Schema(description = "我的友链视图对象")
public class MyFriendLinkVO {

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

    @Schema(description = "拒绝理由，仅未通过时有值")
    private String rejectReason;

    @Schema(description = "提交时间")
    private OffsetDateTime createTime;
}
