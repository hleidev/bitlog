package top.harrylei.community.user.config;

import com.fasterxml.jackson.databind.ObjectMapper;
import jakarta.servlet.http.HttpServletResponse;
import lombok.RequiredArgsConstructor;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.http.MediaType;
import org.springframework.security.config.annotation.web.builders.HttpSecurity;
import org.springframework.security.config.annotation.web.configuration.EnableWebSecurity;
import org.springframework.security.config.annotation.web.configurers.AbstractHttpConfigurer;
import org.springframework.security.config.http.SessionCreationPolicy;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.security.web.SecurityFilterChain;
import org.springframework.security.web.authentication.UsernamePasswordAuthenticationFilter;
import top.harrylei.community.common.enums.ResultCode;
import top.harrylei.community.common.model.Result;
import top.harrylei.community.user.filter.GatewayAuthenticationFilter;

/**
 * Spring Security 配置
 *
 * @author harry
 * @since 0.0.1
 */
@Configuration
@EnableWebSecurity
@RequiredArgsConstructor
public class SecurityConfig {

    private final GatewayAuthenticationFilter gatewayAuthenticationFilter;
    private final ObjectMapper objectMapper;

    @Bean
    public SecurityFilterChain filterChain(HttpSecurity http) throws Exception {
        http
                .csrf(AbstractHttpConfigurer::disable)
                .sessionManagement(session ->
                        session.sessionCreationPolicy(SessionCreationPolicy.STATELESS))
                .authorizeHttpRequests(auth -> auth
                        .requestMatchers(
                                "/api/v1/auth/login",
                                "/api/v1/auth/register",
                                "/api/v1/internal/**",
                                "/actuator/**",
                                "/swagger-ui/**",
                                "/v3/api-docs/**"
                        ).permitAll()
                        .anyRequest().authenticated()
                )
                .formLogin(AbstractHttpConfigurer::disable)
                .httpBasic(AbstractHttpConfigurer::disable)
                .exceptionHandling(ex -> ex
                        // 未登录：返回 401 JSON
                        .authenticationEntryPoint((request, response, e) -> {
                            try {
                                writeJson(response, HttpServletResponse.SC_UNAUTHORIZED,
                                        Result.fail(ResultCode.TOKEN_INVALID));
                            } catch (Exception ex2) {
                                response.sendError(HttpServletResponse.SC_UNAUTHORIZED);
                            }
                        })
                        // 已登录但权限不足：返回 403 JSON
                        .accessDeniedHandler((request, response, e) -> {
                            try {
                                writeJson(response, HttpServletResponse.SC_FORBIDDEN,
                                        Result.fail(ResultCode.FORBIDDEN));
                            } catch (Exception ex2) {
                                response.sendError(HttpServletResponse.SC_FORBIDDEN);
                            }
                        })
                )
                .addFilterBefore(gatewayAuthenticationFilter, UsernamePasswordAuthenticationFilter.class);

        return http.build();
    }

    @Bean
    public PasswordEncoder passwordEncoder() {
        return new BCryptPasswordEncoder();
    }

    private void writeJson(HttpServletResponse response, int status, Object body) throws Exception {
        response.setStatus(status);
        response.setContentType(MediaType.APPLICATION_JSON_VALUE);
        response.setCharacterEncoding("UTF-8");
        response.getWriter().write(objectMapper.writeValueAsString(body));
    }
}
