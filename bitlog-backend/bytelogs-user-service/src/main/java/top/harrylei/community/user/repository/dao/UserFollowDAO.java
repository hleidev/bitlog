package top.harrylei.community.user.repository.dao;

import com.baomidou.mybatisplus.core.metadata.IPage;
import com.baomidou.mybatisplus.extension.service.impl.ServiceImpl;
import org.springframework.stereotype.Repository;
import top.harrylei.community.api.enums.common.DeleteStatusEnum;
import top.harrylei.community.api.enums.user.UserFollowStatusEnum;
import top.harrylei.community.api.model.user.req.UserFollowQueryParam;
import top.harrylei.community.api.model.user.vo.UserFollowVO;
import top.harrylei.community.user.repository.entity.UserFollowDO;
import top.harrylei.community.user.repository.mapper.UserFollowMapper;

import java.util.List;

/**
 * 用户关注数据访问对象
 *
 * @author harry
 * @since 0.0.1
 */
@Repository
public class UserFollowDAO extends ServiceImpl<UserFollowMapper, UserFollowDO> {

    public UserFollowDO getFollowRelation(Long userId, Long followUserId) {
        if (userId == null || followUserId == null) return null;
        return lambdaQuery()
                .eq(UserFollowDO::getUserId, userId)
                .eq(UserFollowDO::getFollowUserId, followUserId)
                .eq(UserFollowDO::getDeleted, DeleteStatusEnum.NOT_DELETED)
                .one();
    }

    public boolean updateFollowStatus(Long userId, Long followUserId, UserFollowStatusEnum status) {
        if (userId == null || followUserId == null || status == null) return false;
        return lambdaUpdate()
                .eq(UserFollowDO::getUserId, userId)
                .eq(UserFollowDO::getFollowUserId, followUserId)
                .eq(UserFollowDO::getDeleted, DeleteStatusEnum.NOT_DELETED)
                .set(UserFollowDO::getFollowState, status)
                .update();
    }

    public List<Long> listFollowerIds(Long userId) {
        if (userId == null) return List.of();
        return lambdaQuery()
                .select(UserFollowDO::getUserId)
                .eq(UserFollowDO::getFollowUserId, userId)
                .eq(UserFollowDO::getFollowState, UserFollowStatusEnum.FOLLOWED)
                .eq(UserFollowDO::getDeleted, DeleteStatusEnum.NOT_DELETED)
                .list()
                .stream()
                .map(UserFollowDO::getUserId)
                .toList();
    }

    public IPage<UserFollowVO> pageFollowingList(UserFollowQueryParam queryParam, IPage<UserFollowVO> page) {
        return getBaseMapper().pageFollowingList(queryParam, page);
    }

    public IPage<UserFollowVO> pageFollowersList(UserFollowQueryParam queryParam, IPage<UserFollowVO> page) {
        return getBaseMapper().pageFollowersList(queryParam, page);
    }
}
