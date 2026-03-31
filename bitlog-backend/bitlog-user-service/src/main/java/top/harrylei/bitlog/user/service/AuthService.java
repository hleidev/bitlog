package top.harrylei.bitlog.user.service;

import top.harrylei.bitlog.api.enums.user.UserRoleEnum;

/**
 * 认证服务接口
 *
 * @author harry
 * @since 0.0.1
 */
public interface AuthService {

    /**
     * 用户注册
     *
     * @param username 用户名
     * @param password 密码
     * @param userRole 用户角色
     */
    void register(String username, String password, UserRoleEnum userRole);

    /**
     * 用户登录
     *
     * @param username  用户名
     * @param password  密码
     * @param keepLogin 是否保持登录
     * @return JWT token
     */
    String login(String username, String password, boolean keepLogin);

    /**
     * 退出登录
     *
     * @param userId 用户 ID
     */
    void logout(Long userId);
}
