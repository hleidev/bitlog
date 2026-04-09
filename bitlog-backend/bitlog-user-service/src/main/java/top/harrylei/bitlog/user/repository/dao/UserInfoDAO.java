package top.harrylei.bitlog.user.repository.dao;

import com.baomidou.mybatisplus.extension.service.impl.ServiceImpl;
import org.springframework.stereotype.Repository;
import top.harrylei.bitlog.common.enums.DeleteStatusEnum;
import top.harrylei.bitlog.user.repository.entity.UserInfoDO;
import top.harrylei.bitlog.user.repository.mapper.UserInfoMapper;

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
        return lambdaQuery()
                .eq(UserInfoDO::getUserId, userId)
                .eq(UserInfoDO::getDeleted, DeleteStatusEnum.NOT_DELETED)
                .one();
    }

    public List<UserInfoDO> listByUserIds(List<Long> userIds) {
        return lambdaQuery()
                .in(UserInfoDO::getUserId, userIds)
                .eq(UserInfoDO::getDeleted, DeleteStatusEnum.NOT_DELETED)
                .list();
    }

    public void updateInfo(Long userId, String userName, String profile, String position, String company) {
        lambdaUpdate()
                .eq(UserInfoDO::getUserId, userId)
                .set(UserInfoDO::getUserName, userName)
                .set(profile != null, UserInfoDO::getProfile, profile)
                .set(position != null, UserInfoDO::getPosition, position)
                .set(company != null, UserInfoDO::getCompany, company)
                .update();
    }

    public void updateAvatar(Long userId, String avatar) {
        lambdaUpdate()
                .eq(UserInfoDO::getUserId, userId)
                .set(UserInfoDO::getAvatar, avatar)
                .update();
    }
}
