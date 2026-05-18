package top.harrylei.bitlog.common.constans;

/**
 * Redis 键前缀常量，统一管理所有 Redis Key 结构
 *
 * @author harry
 * @since 2026-03-17
 */
public class RedisKeyConstants {

    private RedisKeyConstants() {
        throw new IllegalStateException("Constants class");
    }

    // ===== 全局前缀 =====
    public static final String GLOBAL_PREFIX = "bitlog:";

    // ===== 模块前缀 =====
    public static final String USER = GLOBAL_PREFIX + "user:";
    public static final String LOCK = GLOBAL_PREFIX + "lock:";

    // ===== 功能 Key 前缀 =====
    public static final String USER_REFRESH_TOKEN = USER + "refresh:";
    public static final String USER_INFO = USER + "info:";
    public static final String DISTRIBUTED_LOCK = LOCK + "distributed:";
    public static final String DUPLICATE_LOCK = LOCK + "duplicate:";
    public static final String HEALTH_CHECK = GLOBAL_PREFIX + "health:check";
    public static final String LOGIN_FAIL_IP = GLOBAL_PREFIX + "login:fail:ip:";
    public static final String LOGIN_FAIL_USER = GLOBAL_PREFIX + "login:fail:user:";

    // ===== Key 构建方法 =====

    public static String getUserRefreshTokenKey(String tokenValue) {
        return USER_REFRESH_TOKEN + tokenValue;
    }

    public static String getUserInfoKey(Long userId) {
        return USER_INFO + userId;
    }

    public static String getDistributedLockKey(String lockKey) {
        return DISTRIBUTED_LOCK + lockKey;
    }

    public static String getDuplicateLockKey(String lockKey) {
        return DUPLICATE_LOCK + lockKey;
    }

    public static String getLoginFailIpKey(String ip) {
        return LOGIN_FAIL_IP + ip;
    }

    public static String getLoginFailUserKey(String username) {
        return LOGIN_FAIL_USER + username;
    }
}
