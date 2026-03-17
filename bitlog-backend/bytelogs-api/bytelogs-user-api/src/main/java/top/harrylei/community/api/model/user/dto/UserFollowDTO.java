package top.harrylei.community.api.model.user.dto;

import lombok.Data;
import lombok.EqualsAndHashCode;
import top.harrylei.community.api.enums.common.DeleteStatusEnum;
import top.harrylei.community.api.enums.user.UserFollowStatusEnum;
import top.harrylei.community.api.model.base.BaseDTO;

/**
 * 用户关注 DTO
 *
 * @author harry
 * @since 0.0.1
 */
@EqualsAndHashCode(callSuper = true)
@Data
public class UserFollowDTO extends BaseDTO {

    /**
     * 关注者用户ID
     */
    private Long userId;

    /**
     * 被关注者用户ID
     */
    private Long followUserId;

    /**
     * 关注状态
     */
    private UserFollowStatusEnum followState;

    /**
     * 是否删除
     */
    private DeleteStatusEnum deleted;
}
