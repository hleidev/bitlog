package top.harrylei.community.user.repository.mapper;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.baomidou.mybatisplus.core.metadata.IPage;
import org.apache.ibatis.annotations.Mapper;
import top.harrylei.community.api.model.user.req.UserFollowQueryParam;
import top.harrylei.community.api.model.user.vo.UserFollowVO;
import top.harrylei.community.user.repository.entity.UserFollowDO;

/**
 * 用户关注 Mapper
 *
 * @author harry
 * @since 0.0.1
 */
@Mapper
public interface UserFollowMapper extends BaseMapper<UserFollowDO> {

    /**
     * 分页查询用户关注列表
     */
    IPage<UserFollowVO> pageFollowingList(UserFollowQueryParam queryParam, IPage<UserFollowVO> page);

    /**
     * 分页查询用户粉丝列表
     */
    IPage<UserFollowVO> pageFollowersList(UserFollowQueryParam queryParam, IPage<UserFollowVO> page);
}
