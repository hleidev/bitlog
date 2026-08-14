package top.harrylei.bitlog.link.repository.entity;

import com.baomidou.mybatisplus.annotation.TableName;
import lombok.Data;
import lombok.EqualsAndHashCode;
import lombok.experimental.Accessors;
import top.harrylei.bitlog.api.enums.link.FriendLinkStatusEnum;
import top.harrylei.bitlog.common.enums.DeleteStatusEnum;
import top.harrylei.bitlog.common.model.BaseDO;

import java.io.Serial;

/**
 * 友链实体
 *
 * @author Harry
 * @since 2026-08-14
 */
@Data
@EqualsAndHashCode(callSuper = true)
@Accessors(chain = true)
@TableName("friend_link")
public class FriendLinkDO extends BaseDO {

    @Serial
    private static final long serialVersionUID = 1L;

    /**
     * 申请人用户 ID，NULL 表示站长手动录入，无人可自助管理
     */
    private Long userId;

    /**
     * 站点名称
     */
    private String name;

    /**
     * 站点地址
     */
    private String url;

    /**
     * 头像地址，审核通过后转存为自有存储
     */
    private String avatar;

    /**
     * 站点简介
     */
    private String description;

    /**
     * 申请留言
     */
    private String applyMessage;

    /**
     * 拒绝理由，仅申请人本人与管理员可见
     */
    private String rejectReason;

    /**
     * 友链状态：待审核/已通过/已拒绝
     */
    private FriendLinkStatusEnum status;

    /**
     * 是否删除：0-未删除，1-已删除
     */
    private DeleteStatusEnum deleted;
}
