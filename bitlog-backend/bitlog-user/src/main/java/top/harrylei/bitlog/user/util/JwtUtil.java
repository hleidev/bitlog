package top.harrylei.bitlog.user.util;

import io.jsonwebtoken.Jwts;
import io.jsonwebtoken.SignatureAlgorithm;
import io.jsonwebtoken.security.Keys;
import jakarta.annotation.PostConstruct;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;
import top.harrylei.bitlog.api.enums.user.UserRoleEnum;
import top.harrylei.bitlog.common.config.JwtProperties;

import javax.crypto.SecretKey;
import java.util.Date;

/**
 * JWT 工具类
 *
 * @author Harry
 * @since 2026-03-20
 */
@Component
@RequiredArgsConstructor
public class JwtUtil {

    private final JwtProperties jwtProperties;

    private SecretKey secretKey;

    @PostConstruct
    public void init() {
        secretKey = Keys.hmacShaKeyFor(jwtProperties.getSecret().getBytes());
    }

    /**
     * 生成 Access Token
     *
     * @param userId 用户 ID
     * @param role   用户角色
     * @return JWT 字符串（有效期由 jwt.access-token-expire 配置决定）
     */
    public String generateToken(Long userId, UserRoleEnum role) {
        if (userId == null || role == null) {
            throw new IllegalArgumentException("用户 ID 和角色不能为空");
        }
        long now = System.currentTimeMillis();
        Date expiryDate = new Date(now + jwtProperties.getAccessTokenExpire().toMillis());
        return Jwts.builder()
                .setSubject(String.valueOf(userId))
                .claim("role", role)
                .setIssuer(jwtProperties.getIssuer())
                .setIssuedAt(new Date(now))
                .setExpiration(expiryDate)
                .signWith(secretKey, SignatureAlgorithm.HS256)
                .compact();
    }
}
