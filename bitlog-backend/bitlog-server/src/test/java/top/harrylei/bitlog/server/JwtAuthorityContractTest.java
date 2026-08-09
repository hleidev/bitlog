package top.harrylei.bitlog.server;

import io.jsonwebtoken.Jwts;
import io.jsonwebtoken.SignatureAlgorithm;
import io.jsonwebtoken.security.Keys;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.GrantedAuthority;
import org.springframework.security.oauth2.jwt.Jwt;
import org.springframework.security.oauth2.jwt.JwtDecoder;
import org.springframework.security.oauth2.jwt.JwtException;
import org.springframework.security.oauth2.server.resource.authentication.JwtAuthenticationConverter;
import top.harrylei.bitlog.api.enums.user.UserRoleEnum;
import top.harrylei.bitlog.auth.security.JwtTokenIssuer;
import top.harrylei.bitlog.common.config.JwtProperties;
import top.harrylei.bitlog.common.security.SecurityConfig;

import java.util.Date;
import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

/**
 * JWT 权限声明的签发与校验往返契约测试，两端跨模块相连，编译器管不到
 *
 * @author Harry
 * @since 2026-08-09
 */
class JwtAuthorityContractTest {

    /** HS256 要求密钥不短于 256 位 */
    private static final String SECRET = "bitlog-test-secret-key-for-hs256-at-least-32-bytes";

    private static final Long USER_ID = 42L;

    private JwtTokenIssuer issuer;
    private JwtDecoder decoder;
    private JwtAuthenticationConverter converter;

    @BeforeEach
    void setUp() {
        JwtProperties properties = new JwtProperties();
        properties.setSecret(SECRET);
        properties.setIssuer("bitlog");

        issuer = new JwtTokenIssuer(properties);
        issuer.init();

        // 除 JwtProperties 外的协作者本用例用不到，故传 null
        SecurityConfig securityConfig = new SecurityConfig(null, null, null, properties);
        decoder = securityConfig.jwtDecoder();
        converter = securityConfig.jwtAuthenticationConverter();
    }

    @Test
    @DisplayName("管理员令牌解码后授予 ROLE_ADMIN")
    void authenticate_adminToken_grantsRoleAdmin() {
        Authentication authentication = authenticate(issuer.generateToken(USER_ID, UserRoleEnum.ADMIN));

        assertThat(authorities(authentication)).containsExactly("ROLE_ADMIN");
        assertThat(authentication.getName()).isEqualTo(String.valueOf(USER_ID));
    }

    @Test
    @DisplayName("普通用户令牌解码后授予 ROLE_NORMAL，不含管理员权限")
    void authenticate_normalToken_grantsRoleNormalOnly() {
        Authentication authentication = authenticate(issuer.generateToken(USER_ID, UserRoleEnum.NORMAL));

        assertThat(authorities(authentication)).containsExactly("ROLE_NORMAL");
        assertThat(authorities(authentication)).doesNotContain("ROLE_ADMIN");
    }

    @Test
    @DisplayName("权限串不加 SCOPE_ 前缀，否则 hasRole 判定会落空")
    void authenticate_anyToken_keepsAuthorityVerbatim() {
        Authentication authentication = authenticate(issuer.generateToken(USER_ID, UserRoleEnum.ADMIN));

        assertThat(authorities(authentication)).allSatisfy(authority -> assertThat(authority).startsWith("ROLE_"));
    }

    @Test
    @DisplayName("旧版 role 数字声明的令牌解码后无任何权限")
    void authenticate_legacyRoleClaim_grantsNoAuthority() {
        Authentication authentication = authenticate(legacyToken());

        assertThat(authorities(authentication)).isEmpty();
    }

    @Test
    @DisplayName("签名密钥不匹配的令牌被拒绝")
    void decode_tokenSignedWithOtherKey_throws() {
        String foreign = tokenSignedWith("another-secret-key-that-is-long-enough-for-hs256");

        assertThatThrownBy(() -> decoder.decode(foreign)).isInstanceOf(JwtException.class);
    }

    private Authentication authenticate(String token) {
        Jwt jwt = decoder.decode(token);
        return converter.convert(jwt);
    }

    private List<String> authorities(Authentication authentication) {
        return authentication.getAuthorities().stream().map(GrantedAuthority::getAuthority).toList();
    }

    /** 迁移前的令牌形态：role 声明存角色码，无 authorities */
    private String legacyToken() {
        return buildToken(SECRET, builder -> builder.claim("role", 1));
    }

    private String tokenSignedWith(String secret) {
        return buildToken(secret, builder -> builder.claim("authorities", List.of("ROLE_ADMIN")));
    }

    private String buildToken(String secret, java.util.function.UnaryOperator<io.jsonwebtoken.JwtBuilder> customizer) {
        long now = System.currentTimeMillis();
        io.jsonwebtoken.JwtBuilder builder = Jwts.builder().setSubject(String.valueOf(USER_ID))
            .setIssuedAt(new Date(now)).setExpiration(new Date(now + 60_000));
        return customizer.apply(builder).signWith(Keys.hmacShaKeyFor(secret.getBytes()), SignatureAlgorithm.HS256)
            .compact();
    }
}
