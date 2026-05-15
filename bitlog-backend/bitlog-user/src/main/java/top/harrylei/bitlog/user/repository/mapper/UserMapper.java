package top.harrylei.bitlog.user.repository.mapper;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.baomidou.mybatisplus.core.metadata.IPage;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;
import top.harrylei.bitlog.api.model.user.query.UserPageQuery;
import top.harrylei.bitlog.api.model.user.dto.UserDetailDTO;
import top.harrylei.bitlog.api.model.user.dto.UserStatsDTO;
import top.harrylei.bitlog.user.repository.entity.UserDO;

/**
 * 用户账号 Mapper
 *
 * @author harry
 * @since 0.0.1
 */
@Mapper
public interface UserMapper extends BaseMapper<UserDO> {

    /**
     * 联表分页查询用户完整信息
     */
    IPage<UserDetailDTO> pageUsers(@Param("page") IPage<UserDetailDTO> page, @Param("queryParam") UserPageQuery queryParam);

    /**
     * 联表分页查询用户总数
     */
    Long pageUsersCount(@Param("queryParam") UserPageQuery queryParam);

    /**
     * 查询单个用户详细信息
     */
    UserDetailDTO selectUserDetail(Long userId);

    /**
     * 查询各状态用户数量统计
     */
    UserStatsDTO selectUserStats();
}
