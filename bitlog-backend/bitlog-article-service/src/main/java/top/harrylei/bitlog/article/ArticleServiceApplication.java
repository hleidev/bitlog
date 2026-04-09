package top.harrylei.bitlog.article;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.boot.context.properties.ConfigurationPropertiesScan;
import org.springframework.cloud.client.discovery.EnableDiscoveryClient;

/**
 * 文章微服务启动类
 *
 * @author harry
 * @since 0.0.1
 */
@SpringBootApplication(scanBasePackages = "top.harrylei.bitlog")
@EnableDiscoveryClient
@ConfigurationPropertiesScan(basePackages = "top.harrylei.bitlog")
public class ArticleServiceApplication {

    public static void main(String[] args) {
        SpringApplication.run(ArticleServiceApplication.class, args);
    }
}
