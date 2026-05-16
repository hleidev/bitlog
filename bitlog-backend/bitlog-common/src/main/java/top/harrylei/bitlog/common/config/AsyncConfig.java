package top.harrylei.bitlog.common.config;

import org.springframework.context.annotation.Configuration;
import org.springframework.scheduling.annotation.EnableAsync;
import org.springframework.scheduling.annotation.EnableScheduling;

/**
 * 异步与定时任务配置
 *
 * @author Harry
 * @since 2026-03-17
 */
@Configuration
@EnableAsync
@EnableScheduling
public class AsyncConfig {
}
