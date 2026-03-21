package top.harrylei.community.user;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.boot.context.properties.ConfigurationPropertiesScan;
import org.springframework.cloud.client.discovery.EnableDiscoveryClient;

/**
 * 用户微服务启动类
 *
 * @author harry
 * @since 0.0.1
 */
@SpringBootApplication(scanBasePackages = "top.harrylei.community")
@EnableDiscoveryClient
@ConfigurationPropertiesScan(basePackages = "top.harrylei.community")
public class UserServiceApplication {

    public static void main(String[] args) {
        SpringApplication.run(UserServiceApplication.class, args);
    }
}
