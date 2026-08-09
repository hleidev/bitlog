package top.harrylei.bitlog.auth.security;

import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;
import top.harrylei.bitlog.common.constans.RedisKeyConstants;
import top.harrylei.bitlog.common.enums.ResultCode;
import top.harrylei.bitlog.common.exception.BusinessException;
import top.harrylei.bitlog.common.util.RateLimiter;

import java.time.Duration;
import java.util.List;

/**
 * 登录失败限流：IP 与「邮箱+IP」双维度，固定窗口计数
 * <p>
 * 与直接使用 {@link RateLimiter#acquireOrThrow} 的场景不同，登录需要「判定」与「计数」分离—— 只有认证失败才累加，成功则清零，所以这里组合 peek / increment / reset
 * 而非一次性消耗配额。
 * </p>
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
    private static final String UNKNOWN_IP = "unknown";

    private final RateLimiter rateLimiter;

    public void check(String ip, String email) {
        if (ip != null) {
            checkKey(RedisKeyConstants.getLoginFailIpKey(ip), IP_FAIL_MAX);
        }
        checkKey(accountKey(email, ip), USER_FAIL_MAX);
    }

    public void increment(String ip, String email) {
        if (ip != null) {
            rateLimiter.increment(RedisKeyConstants.getLoginFailIpKey(ip), LOCK_DURATION);
        }
        rateLimiter.increment(accountKey(email, ip), LOCK_DURATION);
    }

    public void reset(String ip, String email) {
        List<String> keys = ip != null ? List.of(RedisKeyConstants.getLoginFailIpKey(ip), accountKey(email, ip))
            : List.of(accountKey(email, ip));
        rateLimiter.reset(keys);
    }

    private String accountKey(String email, String ip) {
        return RedisKeyConstants.getLoginFailUserKey(email, ip != null ? ip : UNKNOWN_IP);
    }

    private void checkKey(String key, int max) {
        RateLimiter.Result result = rateLimiter.peek(key, max);
        if (!result.allowed()) {
            throw new BusinessException(ResultCode.LOGIN_TOO_MANY_ATTEMPTS.getCode(),
                "登录失败次数过多，请 " + result.retryAfterSeconds() + " 秒后再试");
        }
    }
}
