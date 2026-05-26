package top.harrylei.bitlog.article.component;

import lombok.RequiredArgsConstructor;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.data.redis.core.script.DefaultRedisScript;
import org.springframework.stereotype.Component;
import top.harrylei.bitlog.common.constans.RedisKeyConstants;

import java.util.List;

/**
 * 文章阅读去重器：基于 IP + 文章 ID，30 分钟内同一 IP 访问同一文章只计一次
 *
 * @author Harry
 * @since 2026-05-26
 */
@Component
@RequiredArgsConstructor
public class ArticleReadDedupe {

    private static final long DEDUP_SECONDS = 30 * 60L;

    private static final DefaultRedisScript<Long> DEDUP_SCRIPT = buildDedupScript();

    private static DefaultRedisScript<Long> buildDedupScript() {
        DefaultRedisScript<Long> script = new DefaultRedisScript<>();
        script.setScriptText("""
            local exists = redis.call('EXISTS', KEYS[1])
            if exists == 1 then
                return 0
            else
                redis.call('SET', KEYS[1], '1', 'EX', ARGV[1])
                return 1
            end""");
        script.setResultType(Long.class);
        return script;
    }

    private final StringRedisTemplate redisTemplate;

    /**
     * 检查是否需要计入阅读数
     *
     * @param articleId 文章 ID
     * @param ip 客户端 IP，null 或空字符串时返回 false
     * @return true 需要计入（首次访问），false 无需计入（重复访问或参数无效）
     */
    public boolean shouldCount(Long articleId, String ip) {
        if (articleId == null || ip == null || ip.isBlank()) {
            return false;
        }
        String key = RedisKeyConstants.getArticleReadKey(articleId, ip);
        Long result = redisTemplate.execute(DEDUP_SCRIPT, List.of(key), String.valueOf(DEDUP_SECONDS));
        return result != null && result == 1L;
    }
}