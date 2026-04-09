package top.harrylei.bitlog.user.service;

import top.harrylei.bitlog.api.enums.user.UserStatusEnum;
import top.harrylei.bitlog.api.model.user.query.UserPageQuery;
import top.harrylei.bitlog.api.model.user.req.PasswordUpdateRequest;
import top.harrylei.bitlog.api.model.user.req.UserUpdateRequest;
import top.harrylei.bitlog.api.model.user.vo.UserDetailVO;
import top.harrylei.bitlog.api.model.user.vo.UserListVO;
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
     * 修改用户状态
     *
     * @param userId 用户 ID
     * @param status 目标状态
     */
    void updateUserStatus(Long userId, UserStatusEnum status);

    /**
     * 分页查询用户列表
     *
     * @param query 查询参数
     * @return 分页结果
     */
    PageVO<UserListVO> pageQuery(UserPageQuery query);
}
