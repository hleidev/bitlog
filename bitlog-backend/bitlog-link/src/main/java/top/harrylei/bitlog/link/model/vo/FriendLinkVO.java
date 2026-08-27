package top.harrylei.bitlog.link.model.vo;

import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Data;
import lombok.experimental.Accessors;

/**
 * 友链公开视图对象
 * <p>
 * 只放公开页卡片用得到的字段。申请留言、拒绝理由、申请人等一律不进这个类， 管理端另有 VO，两者不共用。
 * </p>
 *
 * @author Harry
 * @since 2026-08-14
 */
@Data
@Accessors(chain = true)
@Schema(description = "友链公开视图对象")
public class FriendLinkVO {

    @Schema(description = "友链 ID")
    private Long id;

    @Schema(description = "站点名称")
    private String name;

    @Schema(description = "站点地址")
    private String url;

    @Schema(description = "头像地址，为空时前端退回站名首字")
    private String avatar;

    @Schema(description = "站点简介")
    private String description;
}
