package top.harrylei.bitlog.user.service;

import top.harrylei.bitlog.api.model.user.req.EmailCodeParam;
import top.harrylei.bitlog.api.model.user.req.EmailUpdateParam;
import top.harrylei.bitlog.api.model.user.req.PasswordInitParam;
import top.harrylei.bitlog.api.model.user.req.PasswordUpdateParam;
import top.harrylei.bitlog.api.model.user.vo.PasswordResetVO;
import top.harrylei.bitlog.api.model.user.vo.UserIdentityVO;

import java.util.List;

/**
 * 账号凭据服务
 * <p>
 * 登录后对自身凭据的管理：密码、登录邮箱、第三方绑定。与 {@link AuthService} 的分界是有无登录态—— 后者处理尚未登录时的注册、登录与找回，本服务处理已登录用户改动自己的凭据。
 * </p>
 *
 * @author Harry
 * @since 2026-08-09
 */
public interface AccountCredentialService {

    /**
     * 修改密码
     *
     * @param userId 用户 ID
     * @param req 密码更新请求
     * @param currentRefreshToken 当前设备的 Refresh Token，其余设备一律下线
     */
    void updatePassword(Long userId, PasswordUpdateParam req, String currentRefreshToken);

    /**
     * 首次设置密码，仅限尚无密码的账号（第三方登录建号）
     *
     * @param userId 用户 ID
     * @param req 新密码
     * @param currentRefreshToken 当前设备的 Refresh Token，其余设备一律下线
     */
    void initPassword(Long userId, PasswordInitParam req, String currentRefreshToken);

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
     * 管理员重置用户密码
     * <p>
     * 权限：可重置自己；可重置其他管理员账号；密码重置不涉及权限变更，无限制
     *
     * @param userId 用户 ID
     * @return 重置后的新密码
     */
    PasswordResetVO resetPassword(Long userId);
}
