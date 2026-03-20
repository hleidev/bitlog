package top.harrylei.community.user.repository.mapper;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.baomidou.mybatisplus.core.metadata.IPage;
import org.apache.ibatis.annotations.Mapper;
import top.harrylei.community.api.model.page.param.UserQueryParam;
import top.harrylei.community.api.model.user.dto.UserDetailDTO;
import top.harrylei.community.user.repository.entity.UserDO;

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
    IPage<UserDetailDTO> pageUsers(IPage<UserDetailDTO> page, UserQueryParam queryParam);

    /**
     * 查询单个用户详细信息
     */
    UserDetailDTO selectUserDetail(Long userId);
}
