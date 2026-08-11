package top.harrylei.bitlog.common.config;

import com.alibaba.ttl.threadpool.TtlExecutors;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.context.annotation.Configuration;
import org.springframework.scheduling.annotation.AsyncConfigurer;
import org.springframework.scheduling.annotation.EnableAsync;
import org.springframework.scheduling.annotation.EnableScheduling;

import java.util.concurrent.Executor;

/**
 * 异步与定时任务配置
 *
 * @author Harry
 * @since 2026-03-17
 */
@Configuration
@EnableAsync
@EnableScheduling
public class AsyncConfig implements AsyncConfigurer {

    private final Executor applicationTaskExecutor;

    public AsyncConfig(@Qualifier("applicationTaskExecutor") Executor applicationTaskExecutor) {
        this.applicationTaskExecutor = applicationTaskExecutor;
    }

    /**
     * 必须包装：TransmittableThreadLocal 不加包装时退化为 InheritableThreadLocal，池内线程只在创建时继承一次， 之后复用该线程的任务会读到创建它那个请求的上下文，造成跨请求串号
     */
    @Override
    public Executor getAsyncExecutor() {
        return TtlExecutors.getTtlExecutor(applicationTaskExecutor);
    }
}
