package top.harrylei.bitlog.user.service;

import top.harrylei.bitlog.api.enums.user.UserStatusEnum;
import top.harrylei.bitlog.api.model.user.query.UserPageQuery;
import top.harrylei.bitlog.api.model.user.req.PasswordUpdateRequest;
import top.harrylei.bitlog.api.model.user.req.UserUpdateRequest;
import top.harrylei.bitlog.api.model.user.vo.PasswordResetVO;
import top.harrylei.bitlog.api.model.user.vo.UserDetailVO;
import top.harrylei.bitlog.api.model.user.vo.UserListVO;
import top.harrylei.bitlog.api.model.user.vo.UserStatsVO;
import top.harrylei.bitlog.api.model.user.vo.UserVO;
import top.harrylei.bitlog.common.model.PageVO;

import java.util.List;

/**
 * 用户业务服务接口
 *
 * @author harry
 * @since 0.0.1
 */
public interface UserService {

    /**
     * 根据用户 ID 获取用户基础信息
     *
     * @param userId 用户 ID
     * @return 用户基础信息
     */
    UserVO getUserById(Long userId);

    /**
     * 批量获取用户基础信息
     *
     * @param userIds 用户 ID 列表
     * @return 用户基础信息列表
     */
    List<UserVO> getUserBatchByIds(List<Long> userIds);

    /**
     * 获取用户详情（含关注数/粉丝数）
     *
     * @param userId 用户 ID
     * @return 用户详情
     */
    UserDetailVO getUserDetail(Long userId);

    /**
     * 更新用户基本信息
     *
     * @param userId 用户 ID
     * @param req    更新请求
     */
    void updateUserInfo(Long userId, UserUpdateRequest req);

    /**
     * 修改密码
     *
     * @param userId 用户 ID
     * @param req    密码更新请求
     */
    void updatePassword(Long userId, PasswordUpdateRequest req);

    /**
     * 更新头像
     *
     * @param userId 用户 ID
     * @param avatar 头像 URL
     */
    void updateAvatar(Long userId, String avatar);

    /**
     * 批量修改用户状态
     * <p>权限：不可操作自己（禁用自己将导致无法继续操作）；不可操作管理员账号
     *
     * @param userIds 用户 ID 列表
     * @param status  目标状态
     */
    void updateUserStatusBatch(List<Long> userIds, UserStatusEnum status);

    /**
     * 批量软删除用户
     * <p>权限：可操作自己（管理员也是用户，可注销自己账号）；不可操作其他管理员账号
     *
     * @param userIds 用户 ID 列表
     */
    void deleteUserBatch(List<Long> userIds);

    /**
     * 批量恢复已删除用户
     * <p>权限：可操作自己；不可操作其他管理员账号
     *
     * @param userIds 用户 ID 列表
     */
    void restoreUserBatch(List<Long> userIds);

    /**
     * 批量物理删除用户（不可恢复）
     * <p>权限：可操作自己；不可操作其他管理员账号
     *
     * @param userIds 用户 ID 列表
     */
    void removeUserBatch(List<Long> userIds);

    /**
     * 查询用户数量统计
     *
     * @return 各状态用户数量
     */
    UserStatsVO getUserStats();

    /**
     * 管理员重置用户密码
     * <p>权限：可重置自己；可重置其他管理员账号；密码重置不涉及权限变更，无限制
     *
     * @param userId 用户 ID
     * @return 重置后的新密码
     */
    PasswordResetVO resetPassword(Long userId);

    /**
     * 分页查询用户列表
     *
     * @param query 查询参数
     * @return 分页结果
     */
    PageVO<UserListVO> pageQuery(UserPageQuery query);
}
