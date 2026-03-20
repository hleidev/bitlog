package top.harrylei.community.user.repository.dao;

import com.baomidou.mybatisplus.extension.service.impl.ServiceImpl;
import org.springframework.stereotype.Repository;
import top.harrylei.community.api.enums.common.DeleteStatusEnum;
import top.harrylei.community.user.repository.entity.UserInfoDO;
import top.harrylei.community.user.repository.mapper.UserInfoMapper;

import java.util.List;

/**
 * 用户信息数据访问对象
 *
 * @author harry
 * @since 0.0.1
 */
@Repository
public class UserInfoDAO extends ServiceImpl<UserInfoMapper, UserInfoDO> {

    public UserInfoDO getByUserId(Long userId) {
        if (userId == null) return null;
        return lambdaQuery()
                .eq(UserInfoDO::getUserId, userId)
                .eq(UserInfoDO::getDeleted, DeleteStatusEnum.NOT_DELETED)
                .one();
    }

    public List<UserInfoDO> listByUserIds(List<Long> userIds) {
        if (userIds == null || userIds.isEmpty()) return List.of();
        return lambdaQuery()
                .in(UserInfoDO::getUserId, userIds)
                .eq(UserInfoDO::getDeleted, DeleteStatusEnum.NOT_DELETED)
                .list();
    }
}
