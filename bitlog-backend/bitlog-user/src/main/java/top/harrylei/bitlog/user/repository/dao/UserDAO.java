package top.harrylei.bitlog.user.repository.dao;

import com.baomidou.mybatisplus.core.metadata.IPage;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.baomidou.mybatisplus.extension.service.impl.ServiceImpl;
import org.springframework.stereotype.Repository;
import top.harrylei.bitlog.user.model.dto.UserDetailDTO;
import top.harrylei.bitlog.user.model.dto.UserStatsDTO;
import top.harrylei.bitlog.user.model.query.UserPageParam;
import top.harrylei.bitlog.user.model.enums.UserStatusEnum;
import top.harrylei.bitlog.common.enums.DeleteStatusEnum;
import top.harrylei.bitlog.user.repository.entity.UserDO;
import top.harrylei.bitlog.user.repository.mapper.UserMapper;

import java.util.List;

/**
 * 用户账号数据访问对象
 *
 * @author Harry
 * @since 2026-03-20
 */
@Repository
public class UserDAO extends ServiceImpl<UserMapper, UserDO> {

    /** 注销账号的用户名释放占位，与 uk_user_account_username 的 partial 条件保持一致 */
    public boolean isUsernameTaken(String username) {
        return lambdaQuery().eq(UserDO::getUsername, username).eq(UserDO::getDeleted, DeleteStatusEnum.NOT_DELETED)
            .exists();
    }

    /** 改名场景：排除自己，否则原样提交会被判成重名 */
    public boolean isUsernameTakenByOthers(String username, Long userId) {
        return lambdaQuery().eq(UserDO::getUsername, username).eq(UserDO::getDeleted, DeleteStatusEnum.NOT_DELETED)
            .ne(UserDO::getId, userId).exists();
    }

    public UserDO getByEmail(String email) {
        return lambdaQuery().eq(UserDO::getEmail, email).eq(UserDO::getDeleted, DeleteStatusEnum.NOT_DELETED).one();
    }

    /** 注销账号的邮箱释放占位，与 uk_user_account_email 的 partial 条件保持一致 */
    public boolean isEmailTaken(String email) {
        return lambdaQuery().eq(UserDO::getEmail, email).eq(UserDO::getDeleted, DeleteStatusEnum.NOT_DELETED).exists();
    }

    public UserDO getById(Long userId) {
        return lambdaQuery().eq(UserDO::getId, userId).eq(UserDO::getDeleted, DeleteStatusEnum.NOT_DELETED).one();
    }

    public List<UserDO> listByUserIds(List<Long> userIds) {
        return lambdaQuery().in(UserDO::getId, userIds).eq(UserDO::getDeleted, DeleteStatusEnum.NOT_DELETED).list();
    }

    /** 展示专用：注销后 deleted=1，评论区仍需读到该行才能渲染「已注销用户」 */
    public UserDO getByIdIncludingDeleted(Long userId) {
        return lambdaQuery().eq(UserDO::getId, userId).one();
    }

    /** 展示专用，同 {@link #getByIdIncludingDeleted}；鉴权与写入路径必须走过滤 deleted 的版本 */
    public List<UserDO> listByUserIdsIncludingDeleted(List<Long> userIds) {
        return lambdaQuery().in(UserDO::getId, userIds).list();
    }

    public void updatePassword(Long userId, String encodedPassword) {
        lambdaUpdate().eq(UserDO::getId, userId).set(UserDO::getPassword, encodedPassword).update();
    }

    public void updateUsername(Long userId, String username) {
        lambdaUpdate().eq(UserDO::getId, userId).set(UserDO::getUsername, username).update();
    }

    public void updateEmail(Long userId, String email) {
        lambdaUpdate().eq(UserDO::getId, userId).set(UserDO::getEmail, email).update();
    }

    /** 带 deleted 条件：注销是终态，启停不应把墓碑账号一起改了 */
    public void updateStatusBatch(List<Long> userIds, UserStatusEnum status) {
        lambdaUpdate().in(UserDO::getId, userIds).eq(UserDO::getDeleted, DeleteStatusEnum.NOT_DELETED)
            .set(UserDO::getStatus, status).update();
    }

    /**
     * 注销墓碑：覆写唯一列并置 deleted，腾空 uk_username/uk_email 让原邮箱可重新注册。 密码置空而非改写成随机值——deleted=1 后所有登录查询都过滤该行，空密码本就是第三方建号的常态
     */
    public void deactivate(Long userId, String username, String email) {
        lambdaUpdate().eq(UserDO::getId, userId).set(UserDO::getUsername, username).set(UserDO::getEmail, email)
            .set(UserDO::getPassword, null).set(UserDO::getDeleted, DeleteStatusEnum.DELETED).update();
    }

    public UserStatsDTO countStats(UserPageParam queryParam) {
        return getBaseMapper().selectUserStats(queryParam);
    }

    public IPage<UserDetailDTO> pageUsers(UserPageParam queryParam, Page<UserDetailDTO> page) {
        return getBaseMapper().pageUsers(page, queryParam);
    }

    public UserDetailDTO getUserDetail(Long userId) {
        return getBaseMapper().selectUserDetail(userId);
    }
}
