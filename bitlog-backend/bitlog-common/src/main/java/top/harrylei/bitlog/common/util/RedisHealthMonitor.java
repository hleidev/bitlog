package top.harrylei.bitlog.common.util;

import lombok.Getter;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.autoconfigure.condition.ConditionalOnClass;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;
import top.harrylei.bitlog.common.constans.RedisKeyConstants;

import java.util.concurrent.atomic.AtomicBoolean;

/**
 * Redis 健康状态检查器，定时心跳检测连接是否可用
 *
 * @author harry
 * @since 0.0.1
 */
@Slf4j
@Component
@ConditionalOnClass(name = "org.springframework.data.redis.core.StringRedisTemplate")
@RequiredArgsConstructor
public class RedisHealthMonitor {

    private final StringRedisTemplate stringRedisTemplate;

    @Getter
    @Value("${redis.health.check.enabled:true}")
    private boolean healthCheckEnabled;

    private static final String HEALTH_CHECK_KEY = RedisKeyConstants.HEALTH_CHECK;
    private static final String HEALTH_CHECK_VALUE = "healthy";

    private final AtomicBoolean healthy = new AtomicBoolean(true);

    @Scheduled(fixedRateString = "${redis.health.check.interval:30000}")
    public void performHealthCheck() {
        if (!healthCheckEnabled) return;
        try {
            stringRedisTemplate.opsForValue().set(HEALTH_CHECK_KEY, HEALTH_CHECK_VALUE);
            String result = stringRedisTemplate.opsForValue().get(HEALTH_CHECK_KEY);
            boolean currentHealthy = HEALTH_CHECK_VALUE.equals(result);
            boolean previousHealthy = healthy.getAndSet(currentHealthy);
            if (previousHealthy != currentHealthy) {
                if (currentHealthy) log.info("Redis 健康状态恢复正常");
                else log.error("Redis 健康状态异常，连接不可用");
            }
        } catch (Exception e) {
            if (healthy.getAndSet(false)) log.error("Redis 健康检查失败，连接异常", e);
        }
    }

    public boolean isHealthy() {
        return !healthCheckEnabled || healthy.get();
    }

    public boolean isUnhealthy() {
        return !isHealthy();
    }
}
