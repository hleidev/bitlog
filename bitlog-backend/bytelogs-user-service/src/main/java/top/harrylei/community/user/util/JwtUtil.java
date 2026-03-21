package top.harrylei.community.user.util;

import io.jsonwebtoken.Claims;
import io.jsonwebtoken.JwtException;
import io.jsonwebtoken.Jwts;
import io.jsonwebtoken.SignatureAlgorithm;
import io.jsonwebtoken.security.Keys;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;
import org.springframework.util.StringUtils;
import top.harrylei.community.api.enums.user.UserRoleEnum;
import top.harrylei.community.user.config.JwtProperties;

import javax.crypto.SecretKey;
import java.time.Duration;
import java.util.Date;
import java.util.Optional;

/**
 * JWT 工具类
 *
 * @author harry
 * @since 0.0.1
 */
@Slf4j
@Component
@RequiredArgsConstructor
public class JwtUtil {

    private final JwtProperties jwtProperties;

    private SecretKey secretKey;

    private SecretKey getSecretKey() {
        if (secretKey == null) {
            secretKey = Keys.hmacShaKeyFor(jwtProperties.getSecret().getBytes());
        }
        return secretKey;
    }

    /**
     * 生成 JWT 令牌
     *
     * @param userId    用户 ID
     * @param role      用户角色
     * @param keepLogin 是否保持登录
     * @return JWT 字符串
     */
    public String generateToken(Long userId, UserRoleEnum role, boolean keepLogin) {
        if (userId == null || role == null) {
            throw new IllegalArgumentException("用户 ID 和角色不能为空");
        }
        long now = System.currentTimeMillis();
        Duration expire = keepLogin ? jwtProperties.getKeepLoginExpire() : jwtProperties.getDefaultExpire();
        Date expiryDate = new Date(now + expire.toMillis());
        return Jwts.builder()
                .setSubject(String.valueOf(userId))
                .claim("role", role)
                .setIssuer(jwtProperties.getIssuer())
                .setIssuedAt(new Date(now))
                .setExpiration(expiryDate)
                .signWith(getSecretKey(), SignatureAlgorithm.HS256)
                .compact();
    }

    /**
     * 从令牌中解析用户 ID
     */
    public Long extractUserId(String token) {
        if (!StringUtils.hasText(token)) return null;
        String subject = Optional.ofNullable(parseClaims(token)).map(Claims::getSubject).orElse(null);
        return subject != null ? Long.valueOf(subject) : null;
    }

    /**
     * 从令牌中解析用户角色
     */
    public UserRoleEnum extractUserRole(String token) {
        if (!StringUtils.hasText(token)) {
            return null;
        }
        Integer roleCode = Optional.ofNullable(parseClaims(token))
                .map(claims -> claims.get("role", Integer.class))
                .orElse(null);
        return UserRoleEnum.fromCode(roleCode);
    }

    /**
     * 令牌是否已过期
     */
    public boolean isTokenExpired(String token) {
        if (!StringUtils.hasText(token)) {
            return true;
        }
        Date expiration = Optional.ofNullable(parseClaims(token))
                .map(Claims::getExpiration)
                .orElse(new Date(0));
        return expiration.before(new Date());
    }

    private Claims parseClaims(String token) {
        try {
            return Jwts.parserBuilder()
                    .setSigningKey(getSecretKey())
                    .build()
                    .parseClaimsJws(token)
                    .getBody();
        } catch (JwtException e) {
            log.warn("JWT 解析失败: {}", e.getMessage());
            return null;
        }
    }
}
