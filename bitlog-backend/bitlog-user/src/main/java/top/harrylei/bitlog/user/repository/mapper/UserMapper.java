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
 * @author Harry
 * @since 2026-03-20
 */
@Mapper
public interface UserMapper extends BaseMapper<UserDO> {

    IPage<UserDetailDTO> pageUsers(@Param("page") IPage<UserDetailDTO> page, @Param("queryParam") UserPageQuery queryParam);

    UserDetailDTO selectUserDetail(Long userId);

    UserStatsDTO selectUserStats();
}
