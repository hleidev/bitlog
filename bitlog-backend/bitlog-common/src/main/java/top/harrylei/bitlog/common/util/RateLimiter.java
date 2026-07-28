package top.harrylei.bitlog.common.util;

import lombok.RequiredArgsConstructor;
import org.springframework.boot.autoconfigure.condition.ConditionalOnClass;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.data.redis.core.script.DefaultRedisScript;
import org.springframework.stereotype.Component;

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
        script.setScriptText("""
            local current = redis.call('INCR', KEYS[1])
            if current == 1 then
                redis.call('EXPIRE', KEYS[1], ARGV[2])
            end
            if current > tonumber(ARGV[1]) then
                return 0
            end
            return 1""");
        script.setResultType(Long.class);
        return script;
    }

    private final StringRedisTemplate redisTemplate;

    /**
     * 尝试获取一次执行许可
     *
     * @param key 限流键，需通过 RedisKeyConstants 构建
     * @param limit 窗口内允许的最大次数
     * @param window 窗口长度
     * @return true 允许执行，false 已超出限制或参数无效
     */
    public boolean tryAcquire(String key, int limit, Duration window) {
        if (key == null || key.isBlank() || limit <= 0 || window == null || window.isZero() || window.isNegative()) {
            return false;
        }
        Long result =
            redisTemplate.execute(RATE_SCRIPT, List.of(key), String.valueOf(limit), String.valueOf(window.toSeconds()));
        return result != null && result == 1L;
    }
}
