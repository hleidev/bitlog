package top.harrylei.bitlog.common.security;

import com.fasterxml.jackson.databind.ObjectMapper;
import org.springframework.beans.factory.ObjectProvider;
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
import org.springframework.security.crypto.keygen.Base64StringKeyGenerator;
import org.springframework.security.crypto.keygen.StringKeyGenerator;
import org.springframework.security.oauth2.client.registration.ClientRegistrationRepository;
import org.springframework.security.oauth2.client.web.DefaultOAuth2AuthorizationRequestResolver;
import org.springframework.security.oauth2.client.web.OAuth2AuthorizationRequestResolver;
import org.springframework.security.web.SecurityFilterChain;
import org.springframework.security.web.authentication.UsernamePasswordAuthenticationFilter;
import org.springframework.util.StringUtils;
import org.springframework.web.context.request.RequestAttributes;
import org.springframework.web.context.request.RequestContextHolder;
import org.springframework.web.context.request.ServletRequestAttributes;
import org.springframework.web.cors.CorsConfiguration;
import org.springframework.web.cors.CorsConfigurationSource;
import org.springframework.web.cors.UrlBasedCorsConfigurationSource;
import top.harrylei.bitlog.common.config.JwtProperties;
import top.harrylei.bitlog.common.enums.ResultCode;
import top.harrylei.bitlog.common.model.Result;

import java.util.ArrayList;
import java.util.Base64;
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

    /** 授权入口上标记绑定意图的查询参数，值为 UserService 签发的一次性令牌 */
    private static final String BIND_INTENT_PARAM = "intent";
    private static final StringKeyGenerator STATE_KEY_GENERATOR = new Base64StringKeyGenerator(Base64.getUrlEncoder());

    /**
     * OAuth2 端点统一挂在 /api 下：前端开发服务器只把 /api 代理给后端， 用 Spring 默认的 /oauth2、/login/oauth2 会让授权回调在 dev 环境 404。
     */
    private static final String OAUTH2_AUTHORIZATION_BASE_URI = "/api/oauth2/authorization";
    private static final String OAUTH2_REDIRECTION_BASE_URI = "/api/login/oauth2/code/*";
    private static final List<String> OAUTH2_WHITELIST = List.of("/api/oauth2/**", "/api/login/oauth2/**");

    private final JwtAuthFilter jwtAuthFilter;
    private final ObjectMapper objectMapper;
    private final SecurityProperties securityProperties;

    @Bean
    public SecurityFilterChain filterChain(HttpSecurity http,
        ObjectProvider<ClientRegistrationRepository> clientRegistrationRepository,
        ObjectProvider<OAuth2SuccessHandler> oauth2SuccessHandler,
        ObjectProvider<OAuth2FailureHandler> oauth2FailureHandler) throws Exception {
        List<String> whitelist = new ArrayList<>(BASE_WHITELIST);
        whitelist.addAll(securityProperties.getAdditionalWhitelist());

        // 未配置任何第三方客户端时 ClientRegistrationRepository 不存在，此时跳过 oauth2Login，让后端在缺少 Google 凭据的环境下仍能启动
        ClientRegistrationRepository registrations = clientRegistrationRepository.getIfAvailable();
        boolean oauth2Enabled = registrations != null;
        if (oauth2Enabled) {
            whitelist.addAll(OAUTH2_WHITELIST);
        }

        // 禁用 CSRF token 的前提有两条，缺一不可：
        // 1. 业务接口只认 Authorization: Bearer（见 JwtAuthFilter），跨站请求无法设置该 header；
        // 2. 唯一以 Cookie 为凭据的入口是 /auth/refresh 与 /auth/logout，靠 RefreshTokenCookie 的 SameSite=Lax 拦住跨站 POST
        // 新增任何 Cookie/Session 认证入口、或放宽该 SameSite 时，必须重新评估这里
        http.csrf(AbstractHttpConfigurer::disable).cors(cors -> cors.configurationSource(corsConfigurationSource()))
            .sessionManagement(session -> session.sessionCreationPolicy(SessionCreationPolicy.STATELESS))
            .authorizeHttpRequests(
                auth -> auth.requestMatchers(whitelist.toArray(String[]::new)).permitAll().anyRequest().authenticated())
            .formLogin(AbstractHttpConfigurer::disable).httpBasic(AbstractHttpConfigurer::disable)
            .exceptionHandling(ex -> ex.authenticationEntryPoint((request, response, e) -> handleUnauthorized(response))
                .accessDeniedHandler((request, response, e) -> handleForbidden(response)))
            .addFilterBefore(jwtAuthFilter, UsernamePasswordAuthenticationFilter.class);

        if (oauth2Enabled) {
            http.oauth2Login(oauth2 -> oauth2
                .authorizationEndpoint(
                    endpoint -> endpoint.authorizationRequestResolver(accountSelectingRequestResolver(registrations)))
                .redirectionEndpoint(endpoint -> endpoint.baseUri(OAUTH2_REDIRECTION_BASE_URI))
                .successHandler(oauth2SuccessHandler.getObject()).failureHandler(oauth2FailureHandler.getObject()));
        }

        return http.build();
    }

    /**
     * 强制 Google 每次都展示账号选择器，并把「绑定」意图透传到回调。
     */
    private OAuth2AuthorizationRequestResolver
        accountSelectingRequestResolver(ClientRegistrationRepository registrations) {
        DefaultOAuth2AuthorizationRequestResolver resolver =
            new DefaultOAuth2AuthorizationRequestResolver(registrations, OAUTH2_AUTHORIZATION_BASE_URI);
        resolver.setAuthorizationRequestCustomizer(builder -> {
            builder.additionalParameters(params -> params.put("prompt", "select_account"));
            String intent = currentBindIntent();
            if (StringUtils.hasText(intent)) {
                // 覆盖而非追加：customizer 只拿得到 builder，读不出已生成的 state。
                builder.state(STATE_KEY_GENERATOR.generateKey() + OAuth2SuccessHandler.STATE_INTENT_SEPARATOR + intent);
            }
        });
        return resolver;
    }

    /**
     * 授权入口的 intent 参数标记本次是「绑定」而非「登录」。
     * <p>
     * customizer 签名里拿不到 HttpServletRequest，只能从 RequestContextHolder 取；RequestContextFilter 的 order 早于 Security
     * 过滤器链，此处必然已就绪。
     * </p>
     */
    private static String currentBindIntent() {
        RequestAttributes attributes = RequestContextHolder.getRequestAttributes();
        return attributes instanceof ServletRequestAttributes servletAttributes
            ? servletAttributes.getRequest().getParameter(BIND_INTENT_PARAM) : null;
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
