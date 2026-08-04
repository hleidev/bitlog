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

    /** 展示专用：注销后 deleted=1，评论区仍需读到该行才能渲染「已注销用户」 */
    public UserInfoDO getByUserIdIncludingDeleted(Long userId) {
        return lambdaQuery().eq(UserInfoDO::getUserId, userId).one();
    }

    /** 展示专用，同 {@link #getByUserIdIncludingDeleted}；鉴权与写入路径必须走过滤 deleted 的版本 */
    public List<UserInfoDO> listByUserIdsIncludingDeleted(List<Long> userIds) {
        return lambdaQuery().in(UserInfoDO::getUserId, userIds).list();
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

    /** 注销墓碑：清空全部可识别资料并置 deleted，行本身保留供评论展示 */
    public void anonymize(Long userId) {
        lambdaUpdate().eq(UserInfoDO::getUserId, userId).set(UserInfoDO::getAvatar, "").set(UserInfoDO::getPosition, "")
            .set(UserInfoDO::getCompany, "").set(UserInfoDO::getProfile, "")
            .set(UserInfoDO::getDeleted, DeleteStatusEnum.DELETED).update();
    }
}
