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
 * @author Harry
 * @since 2026-03-20
 */
@Repository
public class UserInfoDAO extends ServiceImpl<UserInfoMapper, UserInfoDO> {

    public UserInfoDO getByUserId(Long userId) {
        return lambdaQuery().eq(UserInfoDO::getUserId, userId).eq(UserInfoDO::getDeleted, DeleteStatusEnum.NOT_DELETED)
            .one();
    }

    public List<UserInfoDO> listByUserIds(List<Long> userIds) {
        return lambdaQuery().in(UserInfoDO::getUserId, userIds).eq(UserInfoDO::getDeleted, DeleteStatusEnum.NOT_DELETED)
            .list();
    }

    /**
     * 更新选填资料。null 表示不更新该字段，空串表示清空——调用方清空输入时须传空串， 传 null 会被当作「不更新」，清空动作将被静默丢弃。
     */
    public void updateInfo(Long userId, String profile, String position, String company) {
        // 三个字段都是选填，全空时 lambdaUpdate 会生成没有 SET 子句的非法 SQL
        if (profile == null && position == null && company == null) {
            return;
        }
        lambdaUpdate().eq(UserInfoDO::getUserId, userId).set(profile != null, UserInfoDO::getProfile, profile)
            .set(position != null, UserInfoDO::getPosition, position)
            .set(company != null, UserInfoDO::getCompany, company).update();
    }

    public void updateAvatar(Long userId, String avatar) {
        lambdaUpdate().eq(UserInfoDO::getUserId, userId).set(UserInfoDO::getAvatar, avatar).update();
    }

    public void removeByUserIds(List<Long> userIds) {
        lambdaUpdate().in(UserInfoDO::getUserId, userIds).remove();
    }
}
