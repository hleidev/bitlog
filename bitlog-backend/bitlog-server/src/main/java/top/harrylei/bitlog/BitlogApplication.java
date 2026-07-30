package top.harrylei.bitlog;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.boot.autoconfigure.security.servlet.UserDetailsServiceAutoConfiguration;
import org.springframework.boot.context.properties.ConfigurationPropertiesScan;
import org.springframework.scheduling.annotation.EnableAsync;

/**
 * 应用启动入口，负责初始化 Spring Boot 上下文
 *
 * @author Harry
 * @since 2026-03-18
 */

@SpringBootApplication(exclude = UserDetailsServiceAutoConfiguration.class)
@ConfigurationPropertiesScan(basePackages = "top.harrylei.bitlog")
@EnableAsync
public class BitlogApplication {
    public static void main(String[] args) {
        SpringApplication.run(BitlogApplication.class, args);
    }
}
