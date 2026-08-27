package top.harrylei.bitlog.auth.model;

import lombok.Data;
import lombok.experimental.Accessors;

/**
 * 第三方登录参数，由 OAuth2 成功处理器从授权服务器返回的用户信息组装
 *
 * @author Harry
 * @since 2026-07-31
 */
@Data
@Accessors(chain = true)
public class OAuthLoginParam {

    /** 平台标识，与 user_identity.provider 对应 */
    private String provider;

    /** 平台侧用户唯一标识，Google 为 sub */
    private String providerUserId;

    private String email;

    /** 平台是否已确认该邮箱归属本人，决定能否按邮箱并入既有账号 */
    private boolean emailVerified;

    /** 平台侧显示名，仅用于首次建号时生成用户名 */
    private String name;

    /** 平台侧头像地址，仅用于首次建号时转存，不入库 */
    private String avatarUrl;
}
