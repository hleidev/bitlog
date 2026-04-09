package top.harrylei.bitlog.common.security;

import com.fasterxml.jackson.databind.ObjectMapper;
import jakarta.servlet.http.HttpServletResponse;
import lombok.RequiredArgsConstructor;
import org.springframework.boot.context.properties.EnableConfigurationProperties;
import org.springframework.context.annotation.Configuration;
import org.springframework.http.MediaType;
import org.springframework.security.config.annotation.method.configuration.EnableMethodSecurity;
import org.springframework.security.config.annotation.web.builders.HttpSecurity;
import org.springframework.security.config.annotation.web.configuration.EnableWebSecurity;
import org.springframework.security.config.annotation.web.configurers.AbstractHttpConfigurer;
import org.springframework.security.config.http.SessionCreationPolicy;
import org.springframework.security.web.SecurityFilterChain;
import org.springframework.security.web.authentication.UsernamePasswordAuthenticationFilter;
import top.harrylei.bitlog.common.enums.ResultCode;
import top.harrylei.bitlog.common.model.Result;

import java.util.ArrayList;
import java.util.List;

/**
 * 通用 Spring Security 配置
 * <p>
 * 预置公共白名单：内部接口、Actuator、Swagger。
 * 各服务通过 security.additional-whitelist 追加自己的公开路径。
 * </p>
 *
 * @author harry
 * @since 0.0.1
 */
@Configuration
@EnableWebSecurity
@EnableMethodSecurity
@RequiredArgsConstructor
@EnableConfigurationProperties(SecurityProperties.class)
public class SecurityConfig {

    /** 所有服务通用的白名单 */
    private static final List<String> BASE_WHITELIST = List.of(
            "/api/v1/internal/**",
            "/actuator/**",
            "/swagger-ui/**",
            "/v3/api-docs/**"
    );

    private final GatewayAuthenticationFilter gatewayAuthenticationFilter;
    private final ObjectMapper objectMapper;
    private final SecurityProperties securityProperties;

    @org.springframework.context.annotation.Bean
    public SecurityFilterChain filterChain(HttpSecurity http) throws Exception {
        List<String> whitelist = new ArrayList<>(BASE_WHITELIST);
        whitelist.addAll(securityProperties.getAdditionalWhitelist());

        http
                .csrf(AbstractHttpConfigurer::disable)
                .sessionManagement(session ->
                        session.sessionCreationPolicy(SessionCreationPolicy.STATELESS))
                .authorizeHttpRequests(auth -> auth
                        .requestMatchers(whitelist.toArray(String[]::new)).permitAll()
                        .anyRequest().authenticated()
                )
                .formLogin(AbstractHttpConfigurer::disable)
                .httpBasic(AbstractHttpConfigurer::disable)
                .exceptionHandling(ex -> ex
                        .authenticationEntryPoint((request, response, e) -> {
                            try {
                                writeJson(response, HttpServletResponse.SC_UNAUTHORIZED,
                                        Result.fail(ResultCode.TOKEN_INVALID));
                            } catch (Exception ex2) {
                                response.sendError(HttpServletResponse.SC_UNAUTHORIZED);
                            }
                        })
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

    private void writeJson(HttpServletResponse response, int status, Object body) throws Exception {
        response.setStatus(status);
        response.setContentType(MediaType.APPLICATION_JSON_VALUE);
        response.setCharacterEncoding("UTF-8");
        response.getWriter().write(objectMapper.writeValueAsString(body));
    }
}
