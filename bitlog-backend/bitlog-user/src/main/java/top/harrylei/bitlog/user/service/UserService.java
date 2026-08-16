package top.harrylei.bitlog.user.service;

import top.harrylei.bitlog.api.enums.user.UserStatusEnum;
import top.harrylei.bitlog.api.model.user.query.UserPageParam;
import top.harrylei.bitlog.api.model.user.req.UserUpdateParam;
import top.harrylei.bitlog.api.model.user.vo.UserDetailVO;
import top.harrylei.bitlog.api.model.user.vo.UserListVO;
import top.harrylei.bitlog.api.model.user.vo.UserStatsVO;
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
    UserStatsVO getUserStats(UserPageParam query);

    /**
     * 分页查询用户列表
     *
     * @param query 查询参数
     * @return 分页结果
     */
    PageVO<UserListVO> pageQuery(UserPageParam query);
}
