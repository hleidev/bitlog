package top.harrylei.community.user.service;

import top.harrylei.community.api.model.user.query.UserFollowPageQuery;
import top.harrylei.community.api.model.user.vo.UserFollowVO;
import top.harrylei.community.common.model.PageVO;

/**
 * 用户关注业务服务接口
 *
 * @author harry
 * @since 0.0.1
 */
public interface UserFollowService {

    /**
     * 关注用户
     *
     * @param currentUserId 当前用户 ID
     * @param followUserId  被关注用户 ID
     */
    void follow(Long currentUserId, Long followUserId);

    /**
     * 取消关注
     *
     * @param currentUserId 当前用户 ID
     * @param followUserId  被取消关注用户 ID
     */
    void unfollow(Long currentUserId, Long followUserId);

    /**
     * 分页查询关注列表
     *
     * @param query 查询参数
     * @return 分页关注列表
     */
    PageVO<UserFollowVO> pageFollowing(UserFollowPageQuery query);

    /**
     * 分页查询粉丝列表
     *
     * @param query 查询参数
     * @return 分页粉丝列表
     */
    PageVO<UserFollowVO> pageFollowers(UserFollowPageQuery query);
}
