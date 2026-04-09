package top.harrylei.bitlog.common.security;

import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import lombok.extern.slf4j.Slf4j;
import org.springframework.lang.NonNull;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.stereotype.Component;
import org.springframework.util.StringUtils;
import org.springframework.web.filter.OncePerRequestFilter;
import top.harrylei.bitlog.common.context.ReqInfoContext;

import java.io.IOException;
import java.util.List;

/**
 * 网关认证过滤器
 * <p>
 * 从 Gateway 转发的请求头中提取用户身份（X-User-Id、X-User-Role），
 * 构建 Spring Security 上下文和 ReqInfoContext，供下游业务直接使用。
 * </p>
 *
 * @author harry
 * @since 0.0.1
 */
@Slf4j
@Component
public class GatewayAuthenticationFilter extends OncePerRequestFilter {

    @Override
    protected void doFilterInternal(@NonNull HttpServletRequest request,
                                    @NonNull HttpServletResponse response,
                                    @NonNull FilterChain filterChain) throws ServletException, IOException {
        try {
            String userId = request.getHeader("X-User-Id");
            String roleCode = request.getHeader("X-User-Role");
            if (StringUtils.hasText(userId) && StringUtils.hasText(roleCode)) {
                buildContext(userId, roleCode, request);
            }
            filterChain.doFilter(request, response);
        } finally {
            SecurityContextHolder.clearContext();
            ReqInfoContext.clear();
        }
    }

    private void buildContext(String userIdStr, String roleCodeStr, HttpServletRequest request) {
        try {
            Long userId = Long.parseLong(userIdStr);
            // 1 = ADMIN，其余视为 NORMAL（与 UserRoleEnum 保持一致，common 不依赖 user-api）
            String roleAuthority = "1".equals(roleCodeStr) ? "ROLE_ADMIN" : "ROLE_NORMAL";

            UsernamePasswordAuthenticationToken authentication = new UsernamePasswordAuthenticationToken(
                    userId, null, List.of(new SimpleGrantedAuthority(roleAuthority)));
            SecurityContextHolder.getContext().setAuthentication(authentication);

            ReqInfoContext.setContext(new ReqInfoContext.ReqInfo()
                    .setUserId(userId)
                    .setAuthorities(List.of(roleAuthority))
                    .setClientIp(getClientIp(request))
                    .setPath(request.getRequestURI()));
        } catch (NumberFormatException e) {
            log.debug("header 解析失败: X-User-Id={}, X-User-Role={}", userIdStr, roleCodeStr);
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
