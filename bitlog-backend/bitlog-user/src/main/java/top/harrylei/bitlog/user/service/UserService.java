package top.harrylei.bitlog.user.service;

import top.harrylei.bitlog.api.enums.user.UserStatusEnum;
import top.harrylei.bitlog.api.model.user.query.UserPageParam;
import top.harrylei.bitlog.api.model.user.req.EmailCodeParam;
import top.harrylei.bitlog.api.model.user.req.EmailUpdateParam;
import top.harrylei.bitlog.api.model.user.req.PasswordInitParam;
import top.harrylei.bitlog.api.model.user.req.PasswordUpdateParam;
import top.harrylei.bitlog.api.model.user.req.UserUpdateParam;
import top.harrylei.bitlog.api.model.user.vo.PasswordResetVO;
import top.harrylei.bitlog.api.model.user.vo.UserDetailVO;
import top.harrylei.bitlog.api.model.user.vo.UserIdentityVO;
import top.harrylei.bitlog.api.model.user.vo.UserListVO;
import top.harrylei.bitlog.api.model.user.vo.UserStatsVO;
import top.harrylei.bitlog.api.model.user.vo.UserVO;
import top.harrylei.bitlog.common.model.PageVO;

import java.util.List;

/**
 * 用户业务服务接口
 *
 * @author Harry
 * @since 2026-03-28
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
     * 获取用户详情，含已注销账号
     * <p>
     * 供管理端查看墓碑账号；{@link #getUserDetail} 仍走过滤 deleted 的查询，天然拒绝已注销账号
     *
     * @param userId 用户 ID
     * @return 用户详情
     */
    UserDetailVO getUserDetailIncludingDeactivated(Long userId);

    /**
     * 更新用户基本信息
     *
     * @param userId 用户 ID
     * @param req 更新请求
     */
    void updateUserInfo(Long userId, UserUpdateParam req);

    /**
     * 修改密码
     *
     * @param userId 用户 ID
     * @param req 密码更新请求
     */
    void updatePassword(Long userId, PasswordUpdateParam req);

    /**
     * 首次设置密码，仅限尚无密码的账号（第三方登录建号）
     *
     * @param userId 用户 ID
     * @param req 新密码
     */
    void initPassword(Long userId, PasswordInitParam req);

    /**
     * 查询已绑定的第三方身份
     *
     * @param userId 用户 ID
     * @return 绑定列表
     */
    List<UserIdentityVO> listIdentities(Long userId);

    /**
     * 创建第三方绑定意图，返回一次性令牌
     * <p>
     * 授权回调不携带业务登录态，令牌用于把回调关联回当前账号
     *
     * @param userId 用户 ID
     * @return 意图令牌
     */
    String createBindIntent(Long userId);

    /**
     * 解绑第三方身份
     * <p>
     * 解绑后须至少保留一种登录方式，否则账号将永久无法登录
     *
     * @param userId 用户 ID
     * @param provider 平台标识
     */
    void unbindIdentity(Long userId, String provider);

    /**
     * 向待绑定的新邮箱发送验证码
     *
     * @param userId 用户 ID
     * @param req 新邮箱
     */
    void sendEmailChangeCode(Long userId, EmailCodeParam req);

    /**
     * 校验验证码并换绑邮箱
     *
     * @param userId 用户 ID
     * @param req 新邮箱与验证码
     */
    void updateEmail(Long userId, EmailUpdateParam req);

    /**
     * 更新头像
     *
     * @param userId 用户 ID
     * @param avatar 头像 URL
     */
    void updateAvatar(Long userId, String avatar);

    /**
     * 批量修改用户状态
     * <p>
     * 权限：不可操作自己（禁用自己将导致无法继续操作）；不可操作管理员账号
     *
     * @param userIds 用户 ID 列表
     * @param status 目标状态
     */
    void updateUserStatusBatch(List<Long> userIds, UserStatusEnum status);

    /**
     * 批量注销用户（墓碑化，不可撤销）
     * <p>
     * 覆写唯一列并匿名化资料，账号行保留供评论继续引用；原邮箱与用户名随之释放，可重新注册。 评论与文章一律保留，需要删除的评论应由用户在注销前自行处理。用户自助注销传入自己的 ID 即可，二次确认由前端负责。
     * 权限：不可操作管理员账号
     *
     * @param userIds 用户 ID 列表
     */
    void deactivateUserBatch(List<Long> userIds);

    /**
     * 查询用户数量统计
     *
     * @return 各状态用户数量
     */
    UserStatsVO getUserStats();

    /**
     * 管理员重置用户密码
     * <p>
     * 权限：可重置自己；可重置其他管理员账号；密码重置不涉及权限变更，无限制
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
    PageVO<UserListVO> pageQuery(UserPageParam query);
}
