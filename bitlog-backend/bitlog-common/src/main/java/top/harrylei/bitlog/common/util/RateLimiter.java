package top.harrylei.bitlog.common.util;

import lombok.RequiredArgsConstructor;
import org.springframework.boot.autoconfigure.condition.ConditionalOnClass;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.data.redis.core.script.DefaultRedisScript;
import org.springframework.stereotype.Component;
import top.harrylei.bitlog.common.enums.ResultCode;
import top.harrylei.bitlog.common.exception.BusinessException;

import java.time.Duration;
import java.util.List;

/**
 * 固定窗口限流器，基于 Redis INCR + EXPIRE 实现
 *
 * @author Harry
 * @since 2026-07-28
 */
@Component
@ConditionalOnClass(name = "org.springframework.data.redis.core.StringRedisTemplate")
@RequiredArgsConstructor
public class RateLimiter {

    private static final DefaultRedisScript<Long> RATE_SCRIPT = buildRateScript();

    private static DefaultRedisScript<Long> buildRateScript() {
        DefaultRedisScript<Long> script = new DefaultRedisScript<>();
        // 被拒时回传剩余秒数，调用方据此告诉用户还要等多久；放行统一返回 -1
        script.setScriptText("""
            local current = redis.call('INCR', KEYS[1])
            if current == 1 then
                redis.call('EXPIRE', KEYS[1], ARGV[2])
            end
            if current > tonumber(ARGV[1]) then
                local ttl = redis.call('TTL', KEYS[1])
                if ttl < 0 then
                    ttl = 0
                end
                return ttl
            end
            return -1""");
        script.setResultType(Long.class);
        return script;
    }

    private final StringRedisTemplate redisTemplate;

    /**
     * 限流结果
     *
     * @param allowed 是否放行
     * @param retryAfterSeconds 被拒时距窗口结束的剩余秒数，放行时为 0
     */
    public record Result(boolean allowed, long retryAfterSeconds) {

        public static Result allow() {
            return new Result(true, 0L);
        }

        public static Result reject(long retryAfterSeconds) {
            return new Result(false, retryAfterSeconds);
        }
    }

    /**
     * 尝试获取一次执行许可
     *
     * @param key 限流键，需通过 RedisKeyConstants 构建
     * @param limit 窗口内允许的最大次数
     * @param window 窗口长度
     * @return 限流结果；参数非法时按拒绝处理
     */
    public Result tryAcquire(String key, int limit, Duration window) {
        if (key == null || key.isBlank() || limit <= 0 || window == null || window.isZero() || window.isNegative()) {
            return Result.reject(0L);
        }
        Long remaining =
            redisTemplate.execute(RATE_SCRIPT, List.of(key), String.valueOf(limit), String.valueOf(window.toSeconds()));
        if (remaining == null) {
            return Result.reject(0L);
        }
        return remaining < 0 ? Result.allow() : Result.reject(remaining);
    }

    /**
     * 尝试获取一次执行许可，超限直接抛业务异常
     *
     * @param key 限流键，需通过 RedisKeyConstants 构建
     * @param limit 窗口内允许的最大次数
     * @param window 窗口长度
     */
    public void acquireOrThrow(String key, int limit, Duration window) {
        Result result = tryAcquire(key, limit, window);
        if (!result.allowed()) {
            throw new BusinessException(ResultCode.TOO_MANY_REQUESTS.getCode(),
                "操作过于频繁，请 " + result.retryAfterSeconds() + " 秒后再试");
        }
    }
}
