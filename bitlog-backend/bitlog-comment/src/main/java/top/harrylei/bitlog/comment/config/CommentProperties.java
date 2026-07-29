package top.harrylei.bitlog.comment.config;

import lombok.Data;
import org.springframework.boot.context.properties.ConfigurationProperties;

import java.time.Duration;

/**
 * 评论配置属性
 *
 * @author Harry
 * @since 2026-07-29
 */
@Data
@ConfigurationProperties(prefix = "bitlog.comment")
public class CommentProperties {

    private RateLimit rateLimit = new RateLimit();

    @Data
    public static class RateLimit {

        /** 相邻两条评论的最小间隔 */
        private Duration minInterval = Duration.ofSeconds(15);

        /** 滚动窗口长度 */
        private Duration window = Duration.ofHours(1);

        /** 窗口内允许发表的最大条数 */
        private int maxPerWindow = 10;
    }
}
