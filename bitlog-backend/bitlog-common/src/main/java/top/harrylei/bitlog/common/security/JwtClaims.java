package top.harrylei.bitlog.common.security;

/**
 * JWT 自定义声明名，签发方与校验方共用，避免两端各写字面量
 *
 * @author Harry
 * @since 2026-08-09
 */
public final class JwtClaims {

    /** 权限串数组，取值形如 ROLE_ADMIN，直接对应 Spring Security 的 GrantedAuthority */
    public static final String AUTHORITIES = "authorities";

    private JwtClaims() {}
}
