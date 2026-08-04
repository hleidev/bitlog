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
    public static final String USER_REFRESH_INDEX = USER + "refresh-index:";
    public static final String USER_INFO = USER + "info:";
    public static final String OAUTH_BIND_INTENT = USER + "oauth:bind:";
    public static final String DISTRIBUTED_LOCK = LOCK + "distributed:";
    public static final String DUPLICATE_LOCK = LOCK + "duplicate:";
    public static final String HEALTH_CHECK = GLOBAL_PREFIX + "health:check";
    public static final String LOGIN_FAIL_IP = GLOBAL_PREFIX + "login:fail:ip:";
    public static final String LOGIN_FAIL_USER = GLOBAL_PREFIX + "login:fail:user:";
    public static final String ARTICLE_READ = GLOBAL_PREFIX + "article:read:";
    public static final String COMMENT_RATE = GLOBAL_PREFIX + "comment:rate:";
    public static final String VERIFY_CODE = USER + "code:";
    public static final String MAIL_RATE = GLOBAL_PREFIX + "mail:rate:";
    public static final String REGISTER_RATE = GLOBAL_PREFIX + "register:rate:";

    // ===== Key 构建方法 =====

    public static String getUserRefreshTokenKey(String tokenValue) {
        return USER_REFRESH_TOKEN + tokenValue;
    }

    /** Refresh Token 按 token 值存，改密/封禁/注销要按用户批量撤销，只能靠这个反向索引找到该用户的所有 token */
    public static String getUserRefreshIndexKey(Long userId) {
        return USER_REFRESH_INDEX + userId;
    }

    public static String getUserInfoKey(Long userId) {
        return USER_INFO + userId;
    }

    /** 第三方绑定意图：授权回调不携带业务登录态，靠这个一次性令牌把回调关联回发起绑定的账号 */
    public static String getOAuthBindIntentKey(String token) {
        return OAUTH_BIND_INTENT + token;
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

    /** 邮箱与 IP 组合：邮箱可枚举，单独按邮箱锁会让攻击者能锁死任意账号 */
    public static String getLoginFailUserKey(String email, String ip) {
        return LOGIN_FAIL_USER + email + ":" + ip;
    }

    public static String getVerifyCodeKey(String purpose, String email) {
        return VERIFY_CODE + purpose + ":" + email;
    }

    public static String getVerifyCodeAttemptsKey(String purpose, String email) {
        return VERIFY_CODE + purpose + ":attempts:" + email;
    }

    /** 按用途隔离：否则刷注册发码可耗尽对方当天配额，令其无法申请找回密码 */
    public static String getMailCooldownKey(String purpose, String email) {
        return MAIL_RATE + "cooldown:" + purpose + ":" + email;
    }

    public static String getMailDailyKey(String purpose, String email) {
        return MAIL_RATE + "daily:" + purpose + ":" + email;
    }

    public static String getMailIpKey(String ip) {
        return MAIL_RATE + "ip:" + ip;
    }

    public static String getRegisterIpKey(String ip) {
        return REGISTER_RATE + "ip:" + ip;
    }

    public static String getArticleReadKey(Long articleId, String ip) {
        return ARTICLE_READ + articleId + ":" + ip;
    }

    public static String getCommentIntervalKey(Long userId) {
        return COMMENT_RATE + "interval:" + userId;
    }

    public static String getCommentHourlyKey(Long userId) {
        return COMMENT_RATE + "hourly:" + userId;
    }
}
