package top.harrylei.bitlog.user.repository.mapper;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.baomidou.mybatisplus.core.metadata.IPage;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;
import top.harrylei.bitlog.user.model.query.UserPageParam;
import top.harrylei.bitlog.user.model.dto.UserDetailDTO;
import top.harrylei.bitlog.user.model.dto.UserStatsDTO;
import top.harrylei.bitlog.user.repository.entity.UserDO;

/**
 * 用户账号 Mapper
 *
 * @author Harry
 * @since 2026-03-20
 */
@Mapper
public interface UserMapper extends BaseMapper<UserDO> {

    IPage<UserDetailDTO> pageUsers(@Param("page") IPage<UserDetailDTO> page,
        @Param("queryParam") UserPageParam queryParam);

    UserDetailDTO selectUserDetail(Long userId);

    UserStatsDTO selectUserStats(@Param("queryParam") UserPageParam queryParam);
}
