package top.harrylei.bitlog.user.service;

import top.harrylei.bitlog.api.enums.user.UserRoleEnum;
import top.harrylei.bitlog.api.model.user.dto.UserAccountDTO;

/**
 * 账号主体服务
 * <p>
 * 认证侧读写 user_account / user_info 的唯一入口。认证只负责凭据的产生与校验， 账号主体的存续与唯一性约束由本服务把守，两侧不共享 DAO。
 * </p>
 *
 * @author Harry
 * @since 2026-08-09
 */
public interface UserAccountService {

    /**
     * 按用户 ID 查账号，已注销账号返回 null
     *
     * @param userId 用户ID
     * @return 账号主体，不存在时为 null
     */
    UserAccountDTO getById(Long userId);

    /**
     * 按邮箱查账号，已注销账号返回 null
     *
     * @param email 归一化后的邮箱
     * @return 账号主体，不存在时为 null
     */
    UserAccountDTO getByEmail(String email);

    /**
     * 邮箱是否已被占用。不区分账号是否已注销：唯一索引不含 deleted，墓碑账号的邮箱仍占位
     *
     * @param email 归一化后的邮箱
     * @return 已占用返回 true
     */
    boolean isEmailTaken(String email);

    /**
     * 校验邮箱与用户名均可用于建号，任一不可用即抛业务异常
     *
     * @param email 归一化后的邮箱
     * @param username 用户名
     */
    void checkAccountAvailable(String email, String username);

    /**
     * 建号：同时落 user_account 与 user_info 两行
     *
     * @param email 归一化后的邮箱
     * @param username 用户名
     * @param encodedPassword 已编码的密码，第三方登录建号时传 null
     * @param role 用户角色
     * @return 新建账号的用户ID
     */
    Long createAccount(String email, String username, String encodedPassword, UserRoleEnum role);

    /**
     * 更新选填资料，null 表示不更新该字段
     *
     * @param userId 用户ID
     * @param profile 个人简介
     * @param position 职位
     * @param company 公司
     */
    void updateProfile(Long userId, String profile, String position, String company);

    /**
     * 覆写密码。调用方负责编码，并自行决定是否撤销既有会话
     *
     * @param userId 用户ID
     * @param encodedPassword 已编码的密码
     */
    void updatePassword(Long userId, String encodedPassword);

    /**
     * 生成一个未被占用的用户名，供第三方登录建号使用
     *
     * @param preferredName 第三方返回的昵称，可为空
     * @param email 归一化后的邮箱，可为空
     * @return 可用的用户名
     */
    String generateUsername(String preferredName, String email);
}
