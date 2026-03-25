package top.harrylei.community.user.filter;

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
import top.harrylei.community.api.enums.user.UserRoleEnum;
import top.harrylei.community.common.context.ReqInfoContext;

import java.io.IOException;
import java.util.List;

/**
 * 网关认证过滤器
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
            UserRoleEnum role = UserRoleEnum.fromCode(Integer.parseInt(roleCodeStr));
            String roleAuthority = role != null ? "ROLE_" + role.name() : "ROLE_NORMAL";

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
