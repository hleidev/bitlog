package top.harrylei.community.common.constans;

/**
 * Kafka Topic 常量定义
 *
 * @author harry
 * @since 0.0.1
 */
public class KafkaTopics {

    private KafkaTopics() {
        throw new IllegalStateException("Constants class");
    }

    /**
     * 通知事件：点赞、评论、关注等触发通知
     */
    public static final String NOTIFICATION_EVENTS = "bytelogs-notification-events";

    /**
     * 活跃度事件：触发排行榜积分更新
     */
    public static final String ACTIVITY_RANK_EVENTS = "bytelogs-activity-rank-events";

    /**
     * 系统事件：注册、登录等
     */
    public static final String SYSTEM_EVENTS = "bytelogs-system-events";

    /**
     * 文章统计事件：阅读量、点赞量、收藏量、评论量更新
     */
    public static final String ARTICLE_STATISTICS_EVENTS = "bytelogs-article-statistics-events";

    /**
     * 用户事件：昵称/头像变更，用于同步文章冗余字段
     */
    public static final String USER_EVENTS = "bytelogs-user-events";

    /**
     * 缓存失效广播：通知各服务清理本地 Caffeine 缓存
     */
    public static final String CACHE_INVALIDATION = "bytelogs-cache-invalidation";
}
