package top.harrylei.bitlog.article.component;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.data.redis.core.script.DefaultRedisScript;
import top.harrylei.bitlog.common.constans.RedisKeyConstants;

import java.util.List;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
@DisplayName("ArticleReadDedupe 测试")
class ArticleReadDedupeTest {

    @Mock
    private StringRedisTemplate redisTemplate;

    private ArticleReadDedupe dedupe;

    @BeforeEach
    void setUp() {
        dedupe = new ArticleReadDedupe(redisTemplate);
    }

    @Test
    @DisplayName("articleId 为 null 时应返回 false")
    void shouldCount_nullArticleId_returnsFalse() {
        assertFalse(dedupe.shouldCount(null, "127.0.0.1"));
    }

    @Test
    @DisplayName("ip 为 null 时应返回 false")
    void shouldCount_nullIp_returnsFalse() {
        assertFalse(dedupe.shouldCount(1L, null));
    }

    @Test
    @DisplayName("ip 为空字符串时应返回 false")
    void shouldCount_blankIp_returnsFalse() {
        assertFalse(dedupe.shouldCount(1L, ""));
    }

    @Test
    @DisplayName("ip 为纯空格时应返回 false")
    void shouldCount_whitespaceIp_returnsFalse() {
        assertFalse(dedupe.shouldCount(1L, "   "));
    }

    @Test
    @DisplayName("Redis 返回 1 时应返回 true")
    void shouldCount_redisReturns1_returnsTrue() {
        when(redisTemplate.execute(any(DefaultRedisScript.class), any(List.class), any())).thenReturn(1L);

        assertTrue(dedupe.shouldCount(100L, "192.168.1.1"));
    }

    @Test
    @DisplayName("Redis 返回 0 时应返回 false")
    void shouldCount_redisReturns0_returnsFalse() {
        when(redisTemplate.execute(any(DefaultRedisScript.class), any(List.class), any())).thenReturn(0L);

        assertFalse(dedupe.shouldCount(100L, "192.168.1.1"));
    }

    @Test
    @DisplayName("Redis 返回 null 时应返回 false")
    void shouldCount_redisReturnsNull_returnsFalse() {
        when(redisTemplate.execute(any(DefaultRedisScript.class), any(List.class), any())).thenReturn(null);

        assertFalse(dedupe.shouldCount(100L, "192.168.1.1"));
    }

    @Test
    @DisplayName("正确生成 Redis key")
    void shouldCount_correctKeyGeneration() {
        when(redisTemplate.execute(any(DefaultRedisScript.class), eq(List.of("bitlog:article:read:123:10.0.0.1")),
            any())).thenReturn(1L);

        dedupe.shouldCount(123L, "10.0.0.1");
    }
}