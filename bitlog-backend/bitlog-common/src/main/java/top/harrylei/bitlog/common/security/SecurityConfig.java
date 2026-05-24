package top.harrylei.bitlog.common.security;

import com.fasterxml.jackson.databind.ObjectMapper;
import jakarta.servlet.http.HttpServletResponse;
import lombok.RequiredArgsConstructor;
import org.springframework.boot.context.properties.EnableConfigurationProperties;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.http.MediaType;
import org.springframework.security.config.annotation.method.configuration.EnableMethodSecurity;
import org.springframework.security.config.annotation.web.builders.HttpSecurity;
import org.springframework.security.config.annotation.web.configuration.EnableWebSecurity;
import org.springframework.security.config.annotation.web.configurers.AbstractHttpConfigurer;
import org.springframework.security.config.http.SessionCreationPolicy;
import org.springframework.security.web.SecurityFilterChain;
import org.springframework.security.web.authentication.UsernamePasswordAuthenticationFilter;
import org.springframework.web.cors.CorsConfiguration;
import org.springframework.web.cors.CorsConfigurationSource;
import org.springframework.web.cors.UrlBasedCorsConfigurationSource;
import top.harrylei.bitlog.common.config.JwtProperties;
import top.harrylei.bitlog.common.enums.ResultCode;
import top.harrylei.bitlog.common.model.Result;

import java.util.ArrayList;
import java.util.List;

/**
 * 通用 Spring Security 配置
 *
 * @author Harry
 * @since 2026-03-20
 */
@Configuration
@EnableWebSecurity
@EnableMethodSecurity
@RequiredArgsConstructor
@EnableConfigurationProperties({SecurityProperties.class, JwtProperties.class})
public class SecurityConfig {

    private static final List<String> BASE_WHITELIST = List.of("/swagger-ui/**", "/v3/api-docs/**", "/actuator/health");

    private final JwtAuthFilter jwtAuthFilter;
    private final ObjectMapper objectMapper;
    private final SecurityProperties securityProperties;

    @Bean
    public SecurityFilterChain filterChain(HttpSecurity http) throws Exception {
        List<String> whitelist = new ArrayList<>(BASE_WHITELIST);
        whitelist.addAll(securityProperties.getAdditionalWhitelist());

        http.csrf(AbstractHttpConfigurer::disable).cors(cors -> cors.configurationSource(corsConfigurationSource()))
            .sessionManagement(session -> session.sessionCreationPolicy(SessionCreationPolicy.STATELESS))
            .authorizeHttpRequests(
                auth -> auth.requestMatchers(whitelist.toArray(String[]::new)).permitAll().anyRequest().authenticated())
            .formLogin(AbstractHttpConfigurer::disable).httpBasic(AbstractHttpConfigurer::disable)
            .exceptionHandling(ex -> ex.authenticationEntryPoint((request, response, e) -> handleUnauthorized(response))
                .accessDeniedHandler((request, response, e) -> handleForbidden(response)))
            .addFilterBefore(jwtAuthFilter, UsernamePasswordAuthenticationFilter.class);

        return http.build();
    }

    @Bean
    public CorsConfigurationSource corsConfigurationSource() {
        CorsConfiguration config = new CorsConfiguration();
        config.setAllowedOrigins(securityProperties.getAllowedOrigins());
        config.setAllowedMethods(List.of("GET", "POST", "PUT", "DELETE", "PATCH", "OPTIONS"));
        config.setAllowedHeaders(List.of("*"));
        config.setAllowCredentials(true);
        UrlBasedCorsConfigurationSource source = new UrlBasedCorsConfigurationSource();
        source.registerCorsConfiguration("/**", config);
        return source;
    }

    private void handleUnauthorized(HttpServletResponse response) throws java.io.IOException {
        try {
            writeJson(response, HttpServletResponse.SC_UNAUTHORIZED, Result.fail(ResultCode.TOKEN_INVALID));
        } catch (Exception e) {
            response.sendError(HttpServletResponse.SC_UNAUTHORIZED);
        }
    }

    private void handleForbidden(HttpServletResponse response) throws java.io.IOException {
        try {
            writeJson(response, HttpServletResponse.SC_FORBIDDEN, Result.fail(ResultCode.FORBIDDEN));
        } catch (Exception e) {
            response.sendError(HttpServletResponse.SC_FORBIDDEN);
        }
    }

    private void writeJson(HttpServletResponse response, int status, Object body) throws Exception {
        response.setStatus(status);
        response.setContentType(MediaType.APPLICATION_JSON_VALUE);
        response.setCharacterEncoding("UTF-8");
        response.getWriter().write(objectMapper.writeValueAsString(body));
    }
}
