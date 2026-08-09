package top.harrylei.bitlog.common.security;

/**
 * JWT 自定义声明名，签发方与校验方共用
 *
 * @author Harry
 * @since 2026-08-09
 */
public final class JwtClaims {

    /** 值为权限串数组，形如 ["ROLE_ADMIN"] */
    public static final String AUTHORITIES = "authorities";

    private JwtClaims() {}
}
