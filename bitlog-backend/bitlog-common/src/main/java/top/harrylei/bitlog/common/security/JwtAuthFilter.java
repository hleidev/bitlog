package top.harrylei.bitlog.common.security;

import io.jsonwebtoken.Claims;
import io.jsonwebtoken.JwtException;
import io.jsonwebtoken.Jwts;
import io.jsonwebtoken.security.Keys;
import jakarta.annotation.PostConstruct;
import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.HttpHeaders;
import org.springframework.lang.NonNull;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.stereotype.Component;
import org.springframework.util.StringUtils;
import org.springframework.web.filter.OncePerRequestFilter;
import top.harrylei.bitlog.common.config.JwtProperties;
import top.harrylei.bitlog.common.context.ReqInfoContext;

import javax.crypto.SecretKey;
import java.io.IOException;
import java.util.List;

/**
 * JWT 认证过滤器
 * <p>
 * 从 Authorization: Bearer {token} 提取并验证 JWT， 构建 Spring Security 上下文和 ReqInfoContext，供下游业务直接使用。 替代微服务架构中 Gateway +
 * GatewayAuthenticationFilter 的组合。
 * </p>
 * 
 * @author Harry
 * @since 2026-05-15
 */
@Slf4j
@Component
@RequiredArgsConstructor
public class JwtAuthFilter extends OncePerRequestFilter {

    private static final String BEARER_PREFIX = "Bearer ";
    private static final String ROLE_ADMIN = "ROLE_ADMIN";
    private static final String ROLE_NORMAL = "ROLE_NORMAL";
    private static final String ADMIN_ROLE_CODE = "1";

    private final JwtProperties jwtProperties;
    private SecretKey secretKey;

    @PostConstruct
    public void init() {
        secretKey = Keys.hmacShaKeyFor(jwtProperties.getSecret().getBytes());
    }

    @Override
    protected void doFilterInternal(@NonNull HttpServletRequest request, @NonNull HttpServletResponse response,
        @NonNull FilterChain filterChain) throws ServletException, IOException {
        try {
            ReqInfoContext.setContext(
                new ReqInfoContext.ReqInfo().setClientIp(getClientIp(request)).setPath(request.getRequestURI()));

            String token = extractToken(request);
            if (StringUtils.hasText(token)) {
                Claims claims = parseClaims(token);
                if (claims != null) {
                    buildContext(claims, request);
                }
            }
            filterChain.doFilter(request, response);
        } finally {
            SecurityContextHolder.clearContext();
            ReqInfoContext.clear();
        }
    }

    private void buildContext(Claims claims, HttpServletRequest request) {
        try {
            Long userId = Long.parseLong(claims.getSubject());
            Object roleObj = claims.get("role");
            String roleCode = roleObj != null ? String.valueOf(roleObj) : "";
            String roleAuthority = ADMIN_ROLE_CODE.equals(roleCode) ? ROLE_ADMIN : ROLE_NORMAL;

            UsernamePasswordAuthenticationToken auth = new UsernamePasswordAuthenticationToken(userId, null,
                List.of(new SimpleGrantedAuthority(roleAuthority)));
            SecurityContextHolder.getContext().setAuthentication(auth);

            ReqInfoContext
                .setContext(new ReqInfoContext.ReqInfo().setUserId(userId).setAuthorities(List.of(roleAuthority))
                    .setClientIp(getClientIp(request)).setPath(request.getRequestURI()));
        } catch (Exception e) {
            log.debug("JWT context 构建失败: {}", e.getMessage());
        }
    }

    private String extractToken(HttpServletRequest request) {
        String header = request.getHeader(HttpHeaders.AUTHORIZATION);
        if (StringUtils.hasText(header) && header.startsWith(BEARER_PREFIX)) {
            return header.substring(BEARER_PREFIX.length()).trim();
        }
        return null;
    }

    private Claims parseClaims(String token) {
        try {
            return Jwts.parserBuilder().setSigningKey(secretKey).build().parseClaimsJws(token).getBody();
        } catch (JwtException e) {
            log.debug("JWT 解析失败: {}", e.getMessage());
            return null;
        }
    }

    private String getClientIp(HttpServletRequest request) {
        String xForwardedFor = request.getHeader("X-Forwarded-For");
        if (StringUtils.hasText(xForwardedFor)) {
            return xForwardedFor.split(",")[0].trim();
        }
        return request.getRemoteAddr();
    }
}
