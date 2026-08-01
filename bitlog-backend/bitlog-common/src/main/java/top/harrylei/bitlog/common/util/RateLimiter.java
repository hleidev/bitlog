package top.harrylei.bitlog.common.util;

import lombok.RequiredArgsConstructor;
import org.springframework.boot.autoconfigure.condition.ConditionalOnClass;
import org.springframework.data.redis.core.RedisOperations;
import org.springframework.data.redis.core.script.DefaultRedisScript;
import org.springframework.stereotype.Component;
import top.harrylei.bitlog.common.enums.ResultCode;
import top.harrylei.bitlog.common.exception.BusinessException;

import java.time.Duration;
import java.util.List;
import java.util.concurrent.TimeUnit;

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

    private static final DefaultRedisScript<Long> INCR_SCRIPT = buildIncrScript();

    private static DefaultRedisScript<Long> buildIncrScript() {
        DefaultRedisScript<Long> script = new DefaultRedisScript<>();
        // 只负责累加并在首次落键时设过期，判定留给调用方；
        // INCR 与 EXPIRE 必须同脚本执行，分两次发送时 EXPIRE 若失败，计数键将永不过期
        script.setScriptText("""
            local count = redis.call('INCR', KEYS[1])
            if count == 1 then
                redis.call('EXPIRE', KEYS[1], ARGV[1])
            end
            return count""");
        script.setResultType(Long.class);
        return script;
    }

    private final RedisOperations<String, String> redisTemplate;

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
     * 消耗一次配额并判定
     *
     * @param key 限流键，需通过 RedisKeyConstants 构建
     * @param limit 窗口内允许的最大次数
     * @param window 窗口长度
     * @return 限流结果；参数非法时按拒绝处理
     */
    public Result tryAcquire(String key, int limit, Duration window) {
        if (isInvalid(key, window) || limit <= 0) {
            return Result.reject(0L);
        }
        Long count = redisTemplate.execute(INCR_SCRIPT, List.of(key), String.valueOf(window.toSeconds()));
        if (count == null) {
            return Result.reject(0L);
        }
        return count <= limit ? Result.allow() : Result.reject(remainingSeconds(key));
    }

    /**
     * 消耗一次配额，超限直接抛业务异常
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

    /**
     * 只累加不判定，用于「本次先记账、下次请求再拦」的场景，如登录失败计数
     *
     * @param key 限流键，需通过 RedisKeyConstants 构建
     * @param window 窗口长度
     */
    public void increment(String key, Duration window) {
        if (isInvalid(key, window)) {
            return;
        }
        redisTemplate.execute(INCR_SCRIPT, List.of(key), String.valueOf(window.toSeconds()));
    }

    /**
     * 只读判定，不消耗配额
     *
     * @param key 限流键，需通过 RedisKeyConstants 构建
     * @param limit 窗口内允许的最大次数
     * @return 限流结果，键不存在或计数未达上限时放行
     */
    public Result peek(String key, int limit) {
        if (key == null || key.isBlank() || limit <= 0) {
            return Result.allow();
        }
        String value = redisTemplate.opsForValue().get(key);
        if (value == null) {
            return Result.allow();
        }
        long count;
        try {
            count = Long.parseLong(value);
        } catch (NumberFormatException e) {
            // 键被外部写坏时放行，宁可少拦一次也不要让所有人卡在这里
            return Result.allow();
        }
        return count < limit ? Result.allow() : Result.reject(remainingSeconds(key));
    }

    /**
     * 清空计数，用于成功后重置失败次数
     *
     * @param keys 限流键
     */
    public void reset(List<String> keys) {
        if (keys != null && !keys.isEmpty()) {
            redisTemplate.delete(keys);
        }
    }

    private boolean isInvalid(String key, Duration window) {
        return key == null || key.isBlank() || window == null || window.isZero() || window.isNegative();
    }

    private long remainingSeconds(String key) {
        Long ttl = redisTemplate.getExpire(key, TimeUnit.SECONDS);
        return ttl != null && ttl > 0 ? ttl : 0L;
    }
}
