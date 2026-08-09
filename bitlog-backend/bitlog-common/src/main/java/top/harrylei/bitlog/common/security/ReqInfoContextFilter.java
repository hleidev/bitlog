package top.harrylei.bitlog.common.security;

import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import org.springframework.lang.NonNull;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.GrantedAuthority;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.stereotype.Component;
import org.springframework.util.StringUtils;
import org.springframework.web.filter.OncePerRequestFilter;
import top.harrylei.bitlog.common.context.ReqInfoContext;

import java.io.IOException;
import java.util.List;

/**
 * 请求上下文填充过滤器
 *
 * @author Harry
 * @since 2026-08-09
 */
@Component
public class ReqInfoContextFilter extends OncePerRequestFilter {

    private static final String FORWARDED_FOR = "X-Forwarded-For";

    @Override
    protected void doFilterInternal(@NonNull HttpServletRequest request, @NonNull HttpServletResponse response,
        @NonNull FilterChain filterChain) throws ServletException, IOException {
        try {
            ReqInfoContext.ReqInfo reqInfo =
                new ReqInfoContext.ReqInfo().setClientIp(getClientIp(request)).setPath(request.getRequestURI());
            applyAuthentication(reqInfo);
            ReqInfoContext.setContext(reqInfo);

            filterChain.doFilter(request, response);
        } finally {
            ReqInfoContext.clear();
        }
    }

    /** AnonymousAuthenticationToken 的 isAuthenticated 为 true，排除匿名靠的是 sub 解析失败 */
    private void applyAuthentication(ReqInfoContext.ReqInfo reqInfo) {
        Authentication authentication = SecurityContextHolder.getContext().getAuthentication();
        if (authentication == null || !authentication.isAuthenticated()) {
            return;
        }
        Long userId = parseUserId(authentication.getName());
        if (userId == null) {
            return;
        }
        List<String> authorities =
            authentication.getAuthorities().stream().map(GrantedAuthority::getAuthority).toList();
        reqInfo.setUserId(userId).setAuthorities(authorities);
    }

    private Long parseUserId(String name) {
        try {
            return Long.parseLong(name);
        } catch (NumberFormatException e) {
            return null;
        }
    }

    private String getClientIp(HttpServletRequest request) {
        String xForwardedFor = request.getHeader(FORWARDED_FOR);
        if (StringUtils.hasText(xForwardedFor)) {
            return xForwardedFor.split(",")[0].trim();
        }
        return request.getRemoteAddr();
    }
}
