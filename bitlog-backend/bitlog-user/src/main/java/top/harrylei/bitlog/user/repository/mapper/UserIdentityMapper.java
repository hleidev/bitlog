package top.harrylei.bitlog.user.repository.mapper;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import org.apache.ibatis.annotations.Mapper;
import top.harrylei.bitlog.user.repository.entity.UserIdentityDO;

/**
 * 第三方身份关联 Mapper
 *
 * @author Harry
 * @since 2026-07-31
 */
@Mapper
public interface UserIdentityMapper extends BaseMapper<UserIdentityDO> {}
