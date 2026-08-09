package top.harrylei.bitlog.server;

import com.fasterxml.jackson.databind.ObjectMapper;
import jakarta.servlet.Filter;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.security.oauth2.server.resource.web.authentication.BearerTokenAuthenticationFilter;
import org.springframework.security.web.SecurityFilterChain;
import org.springframework.test.context.junit.jupiter.web.SpringJUnitWebConfig;
import org.springframework.web.servlet.config.annotation.EnableWebMvc;
import top.harrylei.bitlog.common.config.JwtProperties;
import top.harrylei.bitlog.common.security.ReqInfoContextFilter;
import top.harrylei.bitlog.common.security.SecurityConfig;
import top.harrylei.bitlog.common.security.SecurityProperties;

import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * 安全过滤器链装配测试，覆盖 Bean 缺失与过滤器错位这类只在建链时才暴露的问题
 *
 * @author Harry
 * @since 2026-08-09
 */
@SpringJUnitWebConfig(classes = {SecurityConfig.class, SecurityFilterChainAssemblyTest.StubBeans.class})
class SecurityFilterChainAssemblyTest {

    @Autowired
    private SecurityFilterChain securityFilterChain;

    @Test
    @DisplayName("过滤器链可以装配，且包含 Bearer 校验与上下文搬运两个过滤器")
    void filterChain_assembles_withBearerAndReqInfoFilters() {
        List<Class<?>> types = filterTypes();

        assertThat(types).contains(BearerTokenAuthenticationFilter.class, ReqInfoContextFilter.class);
    }

    @Test
    @DisplayName("上下文搬运过滤器排在 Bearer 校验之后，否则读不到认证结果")
    void filterChain_reqInfoFilter_runsAfterBearerFilter() {
        List<Class<?>> types = filterTypes();

        assertThat(types.indexOf(ReqInfoContextFilter.class))
            .isGreaterThan(types.indexOf(BearerTokenAuthenticationFilter.class));
    }

    private List<Class<?>> filterTypes() {
        return securityFilterChain.getFilters().stream().<Class<?>>map(Filter::getClass).toList();
    }

    /** 刻意不注册 ClientRegistrationRepository，走无 Google 凭据时的启动分支 */
    @Configuration
    @EnableWebMvc
    static class StubBeans {

        @Bean
        ObjectMapper objectMapper() {
            return new ObjectMapper();
        }

        @Bean
        SecurityProperties securityProperties() {
            SecurityProperties properties = new SecurityProperties();
            properties.setAllowedOrigins(List.of("http://localhost:5173"));
            properties.setAdditionalWhitelist(List.of("/api/v1/article/*"));
            return properties;
        }

        @Bean
        JwtProperties jwtProperties() {
            JwtProperties properties = new JwtProperties();
            properties.setSecret("bitlog-test-secret-key-for-hs256-at-least-32-bytes");
            properties.setIssuer("bitlog");
            return properties;
        }

        @Bean
        ReqInfoContextFilter reqInfoContextFilter() {
            return new ReqInfoContextFilter();
        }
    }
}
