package top.harrylei.bitlog.user.component;

import lombok.RequiredArgsConstructor;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.data.redis.core.script.DefaultRedisScript;
import org.springframework.stereotype.Component;
import top.harrylei.bitlog.common.constans.RedisKeyConstants;
import top.harrylei.bitlog.common.enums.ResultCode;
import top.harrylei.bitlog.common.exception.BusinessException;

import java.time.Duration;
import java.util.List;
import java.util.Objects;
import java.util.concurrent.TimeUnit;

/**
 * 登录限流组件：IP 和邮箱双维度，固定窗口计数
 *
 * @author Harry
 * @since 2026-05-18
 */
@Component
@RequiredArgsConstructor
public class LoginRateLimiter {

    private static final int IP_FAIL_MAX = 20;
    private static final int USER_FAIL_MAX = 5;
    private static final Duration LOCK_DURATION = Duration.ofMinutes(15);

    private static final String LOCK_SECONDS_STR = String.valueOf(LOCK_DURATION.getSeconds());

    /** key 新建时才设 TTL，不重置已有窗口 */
    private static final DefaultRedisScript<Long> INCR_SCRIPT = buildIncrScript();

    private static DefaultRedisScript<Long> buildIncrScript() {
        DefaultRedisScript<Long> script = new DefaultRedisScript<>();
        script.setScriptText("local count = redis.call('INCR', KEYS[1])\n" + "if count == 1 then\n"
            + "    redis.call('EXPIRE', KEYS[1], ARGV[1])\n" + "end\n" + "return count");
        script.setResultType(Long.class);
        return script;
    }

    private final StringRedisTemplate redisTemplate;

    public void check(String ip, String email) {
        if (ip != null) {
            checkKey(RedisKeyConstants.getLoginFailIpKey(ip), IP_FAIL_MAX);
        }
        checkKey(accountKey(email, ip), USER_FAIL_MAX);
    }

    public void increment(String ip, String email) {
        if (ip != null) {
            incrementKey(RedisKeyConstants.getLoginFailIpKey(ip));
        }
        incrementKey(accountKey(email, ip));
    }

    public void reset(String ip, String email) {
        List<String> keys = ip != null ? List.of(RedisKeyConstants.getLoginFailIpKey(ip), accountKey(email, ip))
            : List.of(accountKey(email, ip));
        redisTemplate.delete(keys);
    }

    private String accountKey(String email, String ip) {
        return RedisKeyConstants.getLoginFailUserKey(email, ip != null ? ip : "unknown");
    }

    private void checkKey(String key, int max) {
        String val = redisTemplate.opsForValue().get(key);
        if (val != null && Integer.parseInt(val) >= max) {
            long ttl =
                Objects.requireNonNullElse(redisTemplate.getExpire(key, TimeUnit.SECONDS), LOCK_DURATION.getSeconds());
            throw new BusinessException(ResultCode.LOGIN_TOO_MANY_ATTEMPTS.getCode(), "登录失败次数过多，请 " + ttl + " 秒后再试");
        }
    }

    private void incrementKey(String key) {
        redisTemplate.execute(INCR_SCRIPT, List.of(key), LOCK_SECONDS_STR);
    }
}
