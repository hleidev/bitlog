package top.harrylei.bitlog.common.config;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.scheduling.concurrent.ThreadPoolTaskExecutor;
import top.harrylei.bitlog.common.context.ReqInfoContext;

import java.util.concurrent.CompletableFuture;
import java.util.concurrent.Executor;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * 异步执行器的请求上下文传播测试
 * <p>
 * 不包装 TtlExecutors 时线程池会把创建它那个请求的上下文一直带下去，后续任务读到他人 userId，本用例钉住该行为。
 * </p>
 *
 * @author Harry
 * @since 2026-08-11
 */
class AsyncContextPropagationTest {

    private static final Long FIRST_USER = 1001L;
    private static final Long SECOND_USER = 2002L;

    @Test
    @DisplayName("复用线程池线程时，异步任务读到的是本次请求的用户而非上一次")
    void getAsyncExecutor_reusedPoolThread_seesCurrentRequestUser() throws Exception {
        ThreadPoolTaskExecutor pool = singleThreadPool();
        try {
            Executor executor = new AsyncConfig(pool).getAsyncExecutor();

            assertThat(userIdSeenBy(executor, FIRST_USER)).isEqualTo(FIRST_USER);
            assertThat(userIdSeenBy(executor, SECOND_USER)).isEqualTo(SECOND_USER);
        } finally {
            pool.shutdown();
        }
    }

    @Test
    @DisplayName("匿名请求的异步任务读不到任何用户")
    void getAsyncExecutor_anonymousRequest_seesNoUser() throws Exception {
        ThreadPoolTaskExecutor pool = singleThreadPool();
        try {
            Executor executor = new AsyncConfig(pool).getAsyncExecutor();
            userIdSeenBy(executor, FIRST_USER);

            assertThat(userIdSeenBy(executor, null)).isNull();
        } finally {
            pool.shutdown();
        }
    }

    private ThreadPoolTaskExecutor singleThreadPool() {
        ThreadPoolTaskExecutor pool = new ThreadPoolTaskExecutor();
        pool.setCorePoolSize(1);
        pool.setMaxPoolSize(1);
        pool.initialize();
        return pool;
    }

    /** 模拟一次请求：设上下文、提交异步任务、按过滤器约定清理 */
    private Long userIdSeenBy(Executor executor, Long userId) throws Exception {
        if (userId != null) {
            ReqInfoContext.setContext(new ReqInfoContext.ReqInfo().setUserId(userId));
        }
        try {
            return CompletableFuture.supplyAsync(() -> ReqInfoContext.getContext().getUserId(), executor).get();
        } finally {
            ReqInfoContext.clear();
        }
    }
}
