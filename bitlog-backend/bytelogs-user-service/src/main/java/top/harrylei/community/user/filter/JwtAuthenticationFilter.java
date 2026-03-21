package top.harrylei.community.user.filter;

import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.lang.NonNull;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.stereotype.Component;
import org.springframework.util.StringUtils;
import org.springframework.web.filter.OncePerRequestFilter;
import top.harrylei.community.api.enums.user.UserRoleEnum;
import top.harrylei.community.common.constans.RedisKeyConstants;
import top.harrylei.community.common.context.ReqInfoContext;
import top.harrylei.community.user.util.JwtUtil;

import java.io.IOException;
import java.util.List;

/**
 * JWT 认证过滤器
 * <p>
 * 从 Authorization 头提取并验证 JWT，验证通过则填充 ReqInfoContext 和 Spring Security 上下文。
 * </p>
 *
 * @author harry
 * @since 0.0.1
 */
@Slf4j
@Component
@RequiredArgsConstructor
public class JwtAuthenticationFilter extends OncePerRequestFilter {

    private static final String BEARER_PREFIX = "Bearer ";

    private final JwtUtil jwtUtil;
    private final StringRedisTemplate redisTemplate;

    @Override
    protected void doFilterInternal(@NonNull HttpServletRequest request,
                                    @NonNull HttpServletResponse response,
                                    @NonNull FilterChain filterChain) throws ServletException, IOException {
        try {
            // TODO: Gateway 就绪后切换为读取 X-User-Id / X-User-Role header，删除 JWT 解析逻辑
            // authenticateFromHeaders(request);
            String token = extractToken(request);
            if (StringUtils.hasText(token)) {
                authenticate(token, request);
            }
            filterChain.doFilter(request, response);
        } finally {
            SecurityContextHolder.clearContext();
            ReqInfoContext.clear();
        }
    }

    /**
     * 从 Authorization 头提取 Bearer token
     */
    private String extractToken(HttpServletRequest request) {
        String header = request.getHeader("Authorization");
        if (StringUtils.hasText(header) && header.startsWith(BEARER_PREFIX)) {
            return header.substring(BEARER_PREFIX.length()).trim();
        }
        return null;
    }

    /**
     * 验证 token 并填充上下文
     */
    private void authenticate(String token, HttpServletRequest request) {
        Long userId = jwtUtil.extractUserId(token);
        if (userId == null) {
            log.debug("JWT 解析失败，跳过认证");
            return;
        }

        // 验证 Redis 中的 token 是否一致（防止登出后复用）
        String storedToken = redisTemplate.opsForValue().get(RedisKeyConstants.getUserTokenKey(userId));
        if (!token.equals(storedToken)) {
            log.debug("Redis token 不匹配或已失效 userId={}", userId);
            return;
        }

        UserRoleEnum role = jwtUtil.extractUserRole(token);
        String roleAuthority = role != null ? "ROLE_" + role.name() : "ROLE_NORMAL";

        // 填充 Spring Security 上下文
        UsernamePasswordAuthenticationToken authentication = new UsernamePasswordAuthenticationToken(
                userId,
                null,
                List.of(new SimpleGrantedAuthority(roleAuthority))
        );
        SecurityContextHolder.getContext().setAuthentication(authentication);

        // 填充请求上下文
        ReqInfoContext.ReqInfo reqInfo = new ReqInfoContext.ReqInfo()
                .setUserId(userId)
                .setAuthorities(List.of(roleAuthority))
                .setClientIp(getClientIp(request))
                .setPath(request.getRequestURI());
        ReqInfoContext.setContext(reqInfo);
    }

    /**
     * Gateway 模式：从 X-User-Id / X-User-Role header 填充上下文（Gateway 就绪后启用）
     */
    @SuppressWarnings("unused")
    private void authenticateFromHeaders(HttpServletRequest request) {
        String userIdHeader = request.getHeader("X-User-Id");
        String roleHeader = request.getHeader("X-User-Role");
        if (!StringUtils.hasText(userIdHeader)) {
            return;
        }
        try {
            Long userId = Long.valueOf(userIdHeader);
            String roleAuthority = StringUtils.hasText(roleHeader) ? roleHeader : "ROLE_NORMAL";

            UsernamePasswordAuthenticationToken authentication = new UsernamePasswordAuthenticationToken(
                    userId, null, List.of(new SimpleGrantedAuthority(roleAuthority)));
            SecurityContextHolder.getContext().setAuthentication(authentication);

            ReqInfoContext.ReqInfo reqInfo = new ReqInfoContext.ReqInfo()
                    .setUserId(userId)
                    .setAuthorities(List.of(roleAuthority))
                    .setClientIp(getClientIp(request))
                    .setPath(request.getRequestURI());
            ReqInfoContext.setContext(reqInfo);
        } catch (NumberFormatException e) {
            log.warn("X-User-Id header 格式非法: {}", userIdHeader);
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
