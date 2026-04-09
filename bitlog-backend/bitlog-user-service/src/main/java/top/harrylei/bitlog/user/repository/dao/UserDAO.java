package top.harrylei.bitlog.user.repository.dao;

import com.baomidou.mybatisplus.core.metadata.IPage;
import com.baomidou.mybatisplus.extension.service.impl.ServiceImpl;
import org.springframework.stereotype.Repository;
import top.harrylei.bitlog.api.model.user.dto.UserDetailDTO;
import top.harrylei.bitlog.api.model.user.query.UserPageQuery;
import top.harrylei.bitlog.common.enums.DeleteStatusEnum;
import top.harrylei.bitlog.user.repository.entity.UserDO;
import top.harrylei.bitlog.user.repository.mapper.UserMapper;

import java.util.List;

/**
 * 用户账号数据访问对象
 *
 * @author harry
 * @since 0.0.1
 */
@Repository
public class UserDAO extends ServiceImpl<UserMapper, UserDO> {

    public UserDO getByUsername(String username) {
        return lambdaQuery()
                .eq(UserDO::getUserName, username)
                .eq(UserDO::getDeleted, DeleteStatusEnum.NOT_DELETED)
                .one();
    }

    public boolean existsUser(String username) {
        return lambdaQuery()
                .eq(UserDO::getUserName, username)
                .eq(UserDO::getDeleted, DeleteStatusEnum.NOT_DELETED)
                .exists();
    }

    public UserDO getById(Long userId) {
        return lambdaQuery()
                .eq(UserDO::getId, userId)
                .eq(UserDO::getDeleted, DeleteStatusEnum.NOT_DELETED)
                .one();
    }

    public List<UserDO> listByUserIds(List<Long> userIds) {
        return lambdaQuery()
                .in(UserDO::getId, userIds)
                .eq(UserDO::getDeleted, DeleteStatusEnum.NOT_DELETED)
                .list();
    }

    public void updatePassword(Long userId, String encodedPassword) {
        lambdaUpdate()
                .eq(UserDO::getId, userId)
                .set(UserDO::getPassword, encodedPassword)
                .update();
    }

    public IPage<UserDetailDTO> pageUsers(UserPageQuery queryParam, IPage<UserDetailDTO> page) {
        return getBaseMapper().pageUsers(page, queryParam);
    }

    public UserDetailDTO getUserDetail(Long userId) {
        return getBaseMapper().selectUserDetail(userId);
    }
}
