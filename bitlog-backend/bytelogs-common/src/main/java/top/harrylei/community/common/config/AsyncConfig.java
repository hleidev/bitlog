package top.harrylei.community.common.config;

import org.springframework.context.annotation.Configuration;
import org.springframework.scheduling.annotation.EnableAsync;
import org.springframework.scheduling.annotation.EnableScheduling;

/**
 * 异步与定时任务配置
 *
 * @author harry
 * @since 0.0.1
 */
@Configuration
@EnableAsync
@EnableScheduling
public class AsyncConfig {
}
