package top.harrylei.bitlog.server;

import io.jsonwebtoken.Jwts;
import io.jsonwebtoken.SignatureAlgorithm;
import io.jsonwebtoken.security.Keys;
import jakarta.servlet.FilterChain;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.mock.web.MockHttpServletRequest;
import org.springframework.mock.web.MockHttpServletResponse;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.GrantedAuthority;
import org.springframework.security.core.context.SecurityContextHolder;
import top.harrylei.bitlog.api.enums.user.UserRoleEnum;
import top.harrylei.bitlog.auth.security.JwtTokenIssuer;
import top.harrylei.bitlog.common.config.JwtProperties;
import top.harrylei.bitlog.common.context.ReqInfoContext;
import top.harrylei.bitlog.common.security.JwtAuthFilter;

import java.util.Date;
import java.util.List;
import java.util.concurrent.atomic.AtomicReference;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * JWT 权限声明的签发与校验往返契约测试
 * <p>
 * 签发在 bitlog-auth、校验在 bitlog-common，两端只靠 {@code authorities} 声明相连，编译器管不到，故在此钉住。
 * </p>
 *
 * @author Harry
 * @since 2026-08-09
 */
class JwtAuthorityContractTest {

    /** HS256 要求密钥不短于 256 位 */
    private static final String SECRET = "bitlog-test-secret-key-for-hs256-at-least-32-bytes";

    private static final Long USER_ID = 42L;

    private JwtTokenIssuer issuer;
    private JwtAuthFilter filter;

    @BeforeEach
    void setUp() {
        JwtProperties properties = new JwtProperties();
        properties.setSecret(SECRET);
        properties.setIssuer("bitlog");

        issuer = new JwtTokenIssuer(properties);
        issuer.init();
        filter = new JwtAuthFilter(properties);
        filter.init();
    }

    @AfterEach
    void tearDown() {
        SecurityContextHolder.clearContext();
        ReqInfoContext.clear();
    }

    @Test
    @DisplayName("管理员令牌经过滤器后授予 ROLE_ADMIN")
    void doFilter_adminToken_grantsRoleAdmin() throws Exception {
        Captured captured = runFilter(issuer.generateToken(USER_ID, UserRoleEnum.ADMIN));

        assertThat(captured.authorities()).containsExactly("ROLE_ADMIN");
        assertThat(captured.userId()).isEqualTo(USER_ID);
        assertThat(captured.admin()).isTrue();
    }

    @Test
    @DisplayName("普通用户令牌经过滤器后授予 ROLE_NORMAL 且非管理员")
    void doFilter_normalToken_grantsRoleNormal() throws Exception {
        Captured captured = runFilter(issuer.generateToken(USER_ID, UserRoleEnum.NORMAL));

        assertThat(captured.authorities()).containsExactly("ROLE_NORMAL");
        assertThat(captured.userId()).isEqualTo(USER_ID);
        assertThat(captured.admin()).isFalse();
    }

    @Test
    @DisplayName("旧版 role 数字声明的令牌不再建立登录态")
    void doFilter_legacyRoleClaim_leavesUnauthenticated() throws Exception {
        Captured captured = runFilter(legacyToken());

        assertThat(captured.authorities()).isEmpty();
        assertThat(captured.userId()).isNull();
        assertThat(captured.admin()).isFalse();
    }

    /**
     * 过滤器在 finally 中清空两个上下文，故断言数据须在链内取出
     */
    private Captured runFilter(String token) throws Exception {
        MockHttpServletRequest request = new MockHttpServletRequest("GET", "/api/v1/article/1");
        request.addHeader("Authorization", "Bearer " + token);

        AtomicReference<Captured> holder = new AtomicReference<>();
        FilterChain chain = (req, res) -> {
            Authentication auth = SecurityContextHolder.getContext().getAuthentication();
            List<String> authorities =
                auth == null ? List.of() : auth.getAuthorities().stream().map(GrantedAuthority::getAuthority).toList();
            ReqInfoContext.ReqInfo reqInfo = ReqInfoContext.getContext();
            holder.set(new Captured(authorities, reqInfo.getUserId(), reqInfo.isAdmin()));
        };

        filter.doFilter(request, new MockHttpServletResponse(), chain);
        return holder.get();
    }

    /** 迁移前的令牌形态：role 声明存角色码，无 authorities */
    private String legacyToken() {
        long now = System.currentTimeMillis();
        return Jwts.builder().setSubject(String.valueOf(USER_ID)).claim("role", 1).setIssuedAt(new Date(now))
            .setExpiration(new Date(now + 60_000))
            .signWith(Keys.hmacShaKeyFor(SECRET.getBytes()), SignatureAlgorithm.HS256).compact();
    }

    private record Captured(List<String> authorities, Long userId, boolean admin) {
    }
}
