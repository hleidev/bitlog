package top.harrylei.bitlog.link.config;

import lombok.Data;
import org.springframework.boot.context.properties.ConfigurationProperties;

import java.time.Duration;

/**
 * 友链配置属性
 *
 * @author Harry
 * @since 2026-08-15
 */
@Data
@ConfigurationProperties(prefix = "bitlog.link")
public class FriendLinkProperties {

    private RateLimit rateLimit = new RateLimit();

    @Data
    public static class RateLimit {

        /** 滚动窗口长度 */
        private Duration window = Duration.ofHours(1);

        /** 窗口内允许的写操作次数（申请、修改、撤回合计） */
        private int maxPerWindow = 10;
    }
}
