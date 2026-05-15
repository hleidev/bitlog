package top.harrylei.bitlog.user.util;

import io.jsonwebtoken.Claims;
import io.jsonwebtoken.ExpiredJwtException;
import io.jsonwebtoken.Jwts;
import io.jsonwebtoken.security.Keys;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.junit.jupiter.MockitoExtension;
import top.harrylei.bitlog.api.enums.user.UserRoleEnum;
import top.harrylei.bitlog.user.config.JwtProperties;

import javax.crypto.SecretKey;
import java.time.Duration;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

@ExtendWith(MockitoExtension.class)
@DisplayName("JwtUtil JWT工具类测试")
class JwtUtilTest {

    private static final String TEST_SECRET = "bitlog-test-secret-key-at-least-32bytes!";
    private static final String TEST_ISSUER = "bitlog-test";

    private JwtUtil jwtUtil;
    private JwtProperties jwtProperties;

    @BeforeEach
    void setUp() {
        jwtProperties = new JwtProperties();
        jwtProperties.setSecret(TEST_SECRET);
        jwtProperties.setIssuer(TEST_ISSUER);
        jwtProperties.setAccessTokenExpire(Duration.ofHours(2));

        jwtUtil = new JwtUtil(jwtProperties);
        jwtUtil.init();
    }

    @Test
    @DisplayName("generateToken_普通用户_生成有效token")
    void generateToken_withNormalUser_generatesValidToken() {
        Long userId = 100L;

        String token = jwtUtil.generateToken(userId, UserRoleEnum.NORMAL);

        assertThat(token).isNotBlank();
        SecretKey key = Keys.hmacShaKeyFor(TEST_SECRET.getBytes());
        Claims claims = Jwts.parserBuilder()
                .setSigningKey(key)
                .build()
                .parseClaimsJws(token)
                .getBody();
        assertThat(claims.getSubject()).isEqualTo("100");
        assertThat(claims.getIssuer()).isEqualTo(TEST_ISSUER);
    }

    @Test
    @DisplayName("generateToken_管理员用户_token中角色为ADMIN")
    void generateToken_withAdminRole_tokenContainsAdminRole() {
        Long userId = 1L;

        String token = jwtUtil.generateToken(userId, UserRoleEnum.ADMIN);

        assertThat(token).isNotBlank();
        SecretKey key = Keys.hmacShaKeyFor(TEST_SECRET.getBytes());
        Claims claims = Jwts.parserBuilder()
                .setSigningKey(key)
                .build()
                .parseClaimsJws(token)
                .getBody();
        assertThat(claims.get("role")).isNotNull();
    }

    @Test
    @DisplayName("generateToken_userId为null_抛出IllegalArgumentException")
    void generateToken_withNullUserId_throwsIllegalArgumentException() {
        assertThatThrownBy(() -> jwtUtil.generateToken(null, UserRoleEnum.NORMAL))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessageContaining("用户 ID 和角色不能为空");
    }

    @Test
    @DisplayName("generateToken_role为null_抛出IllegalArgumentException")
    void generateToken_withNullRole_throwsIllegalArgumentException() {
        assertThatThrownBy(() -> jwtUtil.generateToken(1L, null))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessageContaining("用户 ID 和角色不能为空");
    }

    @Test
    @DisplayName("parseToken_合法token_解析出正确userId")
    void parseToken_withValidToken_parsesCorrectUserId() {
        Long userId = 999L;
        String token = jwtUtil.generateToken(userId, UserRoleEnum.NORMAL);

        SecretKey key = Keys.hmacShaKeyFor(TEST_SECRET.getBytes());
        Claims claims = Jwts.parserBuilder()
                .setSigningKey(key)
                .build()
                .parseClaimsJws(token)
                .getBody();

        assertThat(Long.parseLong(claims.getSubject())).isEqualTo(userId);
    }

    @Test
    @DisplayName("parseToken_非法token_抛出异常")
    void parseToken_withInvalidToken_throwsException() {
        String invalidToken = "this.is.not.a.valid.jwt.token";
        SecretKey key = Keys.hmacShaKeyFor(TEST_SECRET.getBytes());

        assertThatThrownBy(() ->
                Jwts.parserBuilder().setSigningKey(key).build().parseClaimsJws(invalidToken))
                .isInstanceOf(Exception.class);
    }

    @Test
    @DisplayName("parseToken_过期token_抛出ExpiredJwtException")
    void parseToken_withExpiredToken_throwsExpiredJwtException() throws InterruptedException {
        JwtProperties shortExpireProps = new JwtProperties();
        shortExpireProps.setSecret(TEST_SECRET);
        shortExpireProps.setIssuer(TEST_ISSUER);
        shortExpireProps.setAccessTokenExpire(Duration.ofMillis(1));

        JwtUtil shortJwtUtil = new JwtUtil(shortExpireProps);
        shortJwtUtil.init();

        String token = shortJwtUtil.generateToken(1L, UserRoleEnum.NORMAL);

        Thread.sleep(10);

        SecretKey key = Keys.hmacShaKeyFor(TEST_SECRET.getBytes());
        assertThatThrownBy(() ->
                Jwts.parserBuilder().setSigningKey(key).build().parseClaimsJws(token))
                .isInstanceOf(ExpiredJwtException.class);
    }

    @Test
    @DisplayName("generateToken_签发时间早于当前时间")
    void generateToken_issuedAtIsBeforeOrEqualNow() {
        long before = System.currentTimeMillis();

        String token = jwtUtil.generateToken(1L, UserRoleEnum.NORMAL);

        long after = System.currentTimeMillis();
        SecretKey key = Keys.hmacShaKeyFor(TEST_SECRET.getBytes());
        Claims claims = Jwts.parserBuilder().setSigningKey(key).build()
                .parseClaimsJws(token).getBody();

        assertThat(claims.getIssuedAt().getTime()).isBetween(before - 1000, after);
    }
}
