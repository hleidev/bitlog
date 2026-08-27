package top.harrylei.bitlog.auth.security;

import io.jsonwebtoken.Jwts;
import io.jsonwebtoken.SignatureAlgorithm;
import io.jsonwebtoken.security.Keys;
import jakarta.annotation.PostConstruct;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;
import top.harrylei.bitlog.user.model.enums.UserRoleEnum;
import top.harrylei.bitlog.common.config.JwtProperties;
import top.harrylei.bitlog.common.security.JwtClaims;

import javax.crypto.SecretKey;
import java.nio.charset.StandardCharsets;
import java.util.Date;
import java.util.List;

/**
 * JWT 工具类
 *
 * @author Harry
 * @since 2026-03-20
 */
@Component
@RequiredArgsConstructor
public class JwtTokenIssuer {

    private final JwtProperties jwtProperties;

    private SecretKey secretKey;

    @PostConstruct
    public void init() {
        // 编码须与校验侧 SecurityConfig#jwtDecoder 一致，否则密钥不同源
        secretKey = Keys.hmacShaKeyFor(jwtProperties.getSecret().getBytes(StandardCharsets.UTF_8));
    }

    /**
     * 生成 Access Token
     *
     * @param userId 用户 ID
     * @param role 用户角色
     * @return JWT 字符串（有效期由 jwt.access-token-expire 配置决定）
     */
    public String generateToken(Long userId, UserRoleEnum role) {
        if (userId == null || role == null) {
            throw new IllegalArgumentException("用户 ID 和角色不能为空");
        }
        long now = System.currentTimeMillis();
        Date expiryDate = new Date(now + jwtProperties.getAccessTokenExpire().toMillis());
        return Jwts.builder().setSubject(String.valueOf(userId))
            .claim(JwtClaims.AUTHORITIES, List.of(role.getAuthority())).setIssuer(jwtProperties.getIssuer())
            .setIssuedAt(new Date(now)).setExpiration(expiryDate).signWith(secretKey, SignatureAlgorithm.HS256)
            .compact();
    }
}
