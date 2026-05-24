package top.harrylei.bitlog.ai.config;

import org.springframework.boot.autoconfigure.AutoConfiguration;
import org.springframework.boot.context.properties.EnableConfigurationProperties;
import org.springframework.context.annotation.Bean;
import top.harrylei.bitlog.ai.port.AiModelRouter;

/**
 * AI 模块自动配置
 *
 * @author Harry
 * @since 2026-05-24
 */
@AutoConfiguration
@EnableConfigurationProperties(AiProperties.class)
public class AiAutoConfiguration {

    @Bean
    public AiModelRouter aiModelRouter(AiProperties properties) {
        return new ConfigAiModelRouter(properties);
    }
}
