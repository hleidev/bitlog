package top.harrylei.community.common.constans;

/**
 * Redis 键前缀常量，统一管理所有 Redis Key 结构
 *
 * @author harry
 * @since 0.0.1
 */
public class RedisKeyConstants {

    private RedisKeyConstants() {
        throw new IllegalStateException("Constants class");
    }

    // ===== 全局前缀 =====
    public static final String GLOBAL_PREFIX = "byte_logs:";

    // ===== 模块前缀 =====
    public static final String USER = GLOBAL_PREFIX + "user:";
    public static final String LOCK = GLOBAL_PREFIX + "lock:";
    public static final String KAFKA = GLOBAL_PREFIX + "kafka:";
    public static final String ACTIVITY = GLOBAL_PREFIX + "activity:";
    public static final String AI = GLOBAL_PREFIX + "ai:";
    public static final String ARTICLE = GLOBAL_PREFIX + "article:";

    // ===== 功能 Key 前缀 =====
    public static final String USER_TOKEN = USER + "token:";
    public static final String USER_INFO = USER + "info:";
    public static final String DISTRIBUTED_LOCK = LOCK + "distributed:";
    public static final String DUPLICATE_LOCK = LOCK + "duplicate:";
    public static final String KAFKA_IDEMPOTENCY = KAFKA + "idempotency:";
    public static final String ACTIVITY_RANK = ACTIVITY + "rank:";
    public static final String ACTIVITY_DAILY = ACTIVITY + "idempotency:";
    public static final String HEALTH_CHECK = GLOBAL_PREFIX + "health:check";
    public static final String AI_HOURLY_LIMIT = AI + "hourly_limit:";
    public static final String AI_DAILY_USAGE = AI + "daily_usage:";
    public static final String AI_CHAT_CONTEXT = AI + "chat_context:";
    public static final String ARTICLE_READ_LOCK = ARTICLE + "read_count_lock:";

    // ===== Key 构建方法 =====

    public static String getUserTokenKey(Long userId) {
        return USER_TOKEN + userId;
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

    public static String getKafkaIdempotencyKey(String eventId) {
        return KAFKA_IDEMPOTENCY + eventId;
    }

    public static String getActivityTotalRankKey() {
        return ACTIVITY_RANK + "total";
    }

    public static String getActivityDailyRankKey(String date) {
        return ACTIVITY_RANK + "daily:" + date;
    }

    public static String getActivityMonthlyRankKey(String yearMonth) {
        return ACTIVITY_RANK + "monthly:" + yearMonth;
    }

    public static String getActivityDailyKey(Long userId, String date) {
        return ACTIVITY_DAILY + date + ":" + userId;
    }

    public static String getChatHourlyLimitKey(Long userId) {
        return AI_HOURLY_LIMIT + userId;
    }

    public static String getChatDailyUsageKey(Long userId, String date) {
        return AI_DAILY_USAGE + userId + ":" + date;
    }

    public static String getChatContextKey(Long conversationId) {
        return AI_CHAT_CONTEXT + conversationId;
    }

    public static String getArticleReadCountLockKey(Long articleId, String identifier, String type) {
        return ARTICLE_READ_LOCK + articleId + ":" + type + ":" + identifier;
    }
}
