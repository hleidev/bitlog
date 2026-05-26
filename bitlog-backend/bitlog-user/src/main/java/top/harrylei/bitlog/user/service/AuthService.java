package top.harrylei.bitlog.user.service;

import top.harrylei.bitlog.api.enums.user.UserRoleEnum;
import top.harrylei.bitlog.api.model.user.req.AdminCreateUserParam;
import top.harrylei.bitlog.api.model.user.vo.UserCreatedVO;

/**
 * 认证服务接口
 *
 * @author Harry
 * @since 2026-03-20
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
     * @param username 用户名
     * @param password 密码
     * @return 包含 Access Token 和 Refresh Token 的登录结果
     */
    LoginResult login(String username, String password);

    /**
     * 刷新 Token
     * <p>
     * 验证 Refresh Token，轮换生成新的双 Token。
     *
     * @param refreshToken 当前 Refresh Token（来自 HttpOnly Cookie）
     * @return 包含新 Access Token 和新 Refresh Token 的结果
     */
    LoginResult refresh(String refreshToken);

    /**
     * 退出登录
     * <p>
     * 从 Redis 中删除 Refresh Token，使其立即失效。
     *
     * @param refreshToken 当前 Refresh Token（来自 HttpOnly Cookie，可为 null）
     */
    void logout(String refreshToken);

    /**
     * 管理员创建用户
     * <p>
     * 密码由系统自动生成，以明文形式返回给管理员
     *
     * @param req 创建请求
     * @return 包含账号和初始密码的结果
     */
    UserCreatedVO adminCreateUser(AdminCreateUserParam req);
}
