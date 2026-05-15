package top.harrylei.bitlog.user.repository.mapper;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import org.apache.ibatis.annotations.Mapper;
import top.harrylei.bitlog.user.repository.entity.UserInfoDO;

/**
 * 用户信息 Mapper
 *
 * @author harry
 * @since 0.0.1
 */
@Mapper
public interface UserInfoMapper extends BaseMapper<UserInfoDO> {
}
