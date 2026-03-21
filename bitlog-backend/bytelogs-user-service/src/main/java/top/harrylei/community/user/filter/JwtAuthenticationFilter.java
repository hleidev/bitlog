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

    private String extractToken(HttpServletRequest request) {
        String header = request.getHeader("Authorization");
        if (StringUtils.hasText(header) && header.startsWith(BEARER_PREFIX)) {
            return header.substring(BEARER_PREFIX.length()).trim();
        }
        return null;
    }

    private void authenticate(String token, HttpServletRequest request) {
        Long userId = jwtUtil.extractUserId(token);
        if (userId == null) {
            log.debug("JWT 解析失败，跳过认证");
            return;
        }

        String storedToken = redisTemplate.opsForValue().get(RedisKeyConstants.getUserTokenKey(userId));
        if (!token.equals(storedToken)) {
            log.debug("Redis token 不匹配或已失效 userId={}", userId);
            return;
        }

        UserRoleEnum role = jwtUtil.extractUserRole(token);
        String roleAuthority = role != null ? "ROLE_" + role.name() : "ROLE_NORMAL";

        buildContext(userId, roleAuthority, request);
    }

    /**
     * 填充 Spring Security 上下文和请求上下文
     */
    private void buildContext(Long userId, String roleAuthority, HttpServletRequest request) {
        UsernamePasswordAuthenticationToken authentication = new UsernamePasswordAuthenticationToken(
                userId, null, List.of(new SimpleGrantedAuthority(roleAuthority)));
        SecurityContextHolder.getContext().setAuthentication(authentication);

        ReqInfoContext.setContext(new ReqInfoContext.ReqInfo()
                .setUserId(userId)
                .setAuthorities(List.of(roleAuthority))
                .setClientIp(getClientIp(request))
                .setPath(request.getRequestURI()));
    }

    private String getClientIp(HttpServletRequest request) {
        String xForwardedFor = request.getHeader("X-Forwarded-For");
        if (StringUtils.hasText(xForwardedFor)) {
            return xForwardedFor.split(",")[0].trim();
        }
        return request.getRemoteAddr();
    }
}
