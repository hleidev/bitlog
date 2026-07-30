package top.harrylei.bitlog.user.service;

import top.harrylei.bitlog.api.model.auth.LoginParam;
import top.harrylei.bitlog.api.model.auth.PasswordResetParam;
import top.harrylei.bitlog.api.model.auth.RegisterParam;
import top.harrylei.bitlog.api.model.auth.UserCreateParam;
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
     * 发送注册验证码
     *
     * @param email 目标邮箱
     */
    void sendRegisterCode(String email);

    /**
     * 用户注册，验证码校验通过后才建号，因此账号建立即为已验证邮箱
     *
     * @param param 注册参数
     */
    void register(RegisterParam param);

    /**
     * 管理员建号，不需要邮箱验证码
     *
     * @param param 建号参数
     */
    void createUser(UserCreateParam param);

    /**
     * 用户登录
     *
     * @param param 登录参数
     * @return 包含 Access Token 和 Refresh Token 的登录结果
     */
    LoginResult login(LoginParam param);

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
     * 发送重置密码验证码
     * <p>
     * 无论邮箱是否存在都静默返回，避免账号枚举
     *
     * @param email 目标邮箱
     */
    void sendResetPasswordCode(String email);

    /**
     * 用验证码重置密码
     *
     * @param param 重置参数
     */
    void resetPassword(PasswordResetParam param);

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
