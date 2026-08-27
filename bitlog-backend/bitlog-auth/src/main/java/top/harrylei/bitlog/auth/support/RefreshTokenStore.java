package top.harrylei.bitlog.auth.support;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.stereotype.Component;
import org.springframework.util.StringUtils;
import top.harrylei.bitlog.user.model.enums.UserRoleEnum;
import top.harrylei.bitlog.common.config.JwtProperties;
import top.harrylei.bitlog.common.constants.RedisKeyConstants;

import java.util.HashSet;
import java.util.Set;
import java.util.UUID;
import java.util.concurrent.TimeUnit;
import java.util.stream.Collectors;

/**
 * Refresh Token 存储：token 值维度的记录，加一份用户维度的反向索引
 *
 * @author Harry
 * @since 2026-08-04
 */
@Slf4j
@Component
@RequiredArgsConstructor
public class RefreshTokenStore {

    private final StringRedisTemplate redisTemplate;
    private final JwtProperties jwtProperties;

    /**
     * 一把 Refresh Token 所承载的身份
     */
    public record Payload(Long userId, UserRoleEnum role) {
    }

    /**
     * 签发一把新的 Refresh Token
     *
     * @return token 值，由调用方写入 Cookie
     */
    public String issue(Long userId, UserRoleEnum role) {
        String refreshToken = UUID.randomUUID().toString();
        long ttlSeconds = jwtProperties.getRefreshTokenExpire().getSeconds();

        // 先索引后本体：两条命令非原子，中断只该留下无害的索引垃圾成员，
        // 反过来则会留下一把索引里查不到、永远撤销不掉的 Refresh Token
        String indexKey = RedisKeyConstants.getUserRefreshIndexKey(userId);
        redisTemplate.opsForSet().add(indexKey, refreshToken);
        redisTemplate.expire(indexKey, ttlSeconds, TimeUnit.SECONDS);

        redisTemplate.opsForValue().set(RedisKeyConstants.getUserRefreshTokenKey(refreshToken),
            userId + ":" + role.name(), ttlSeconds, TimeUnit.SECONDS);
        return refreshToken;
    }

    /**
     * 取出并撤销一把 Refresh Token，供刷新时轮换使用
     *
     * @return 无效或已被撤销时返回 null
     */
    public Payload consume(String refreshToken) {
        if (!StringUtils.hasText(refreshToken)) {
            return null;
        }
        String storedValue =
            redisTemplate.opsForValue().getAndDelete(RedisKeyConstants.getUserRefreshTokenKey(refreshToken));
        if (storedValue == null) {
            return null;
        }

        String[] parts = storedValue.split(":", 2);
        Long userId = Long.parseLong(parts[0]);
        redisTemplate.opsForSet().remove(RedisKeyConstants.getUserRefreshIndexKey(userId), refreshToken);
        return new Payload(userId, UserRoleEnum.valueOf(parts[1]));
    }

    /**
     * 撤销单把 Refresh Token，供退出登录使用
     */
    public void revoke(String refreshToken) {
        consume(refreshToken);
    }

    /**
     * 撤销该用户的全部 Refresh Token
     *
     * @param exceptToken 需要保留的一把，传 null 表示全部撤销
     * @return 实际撤销的数量
     */
    public int revokeAll(Long userId, String exceptToken) {
        String indexKey = RedisKeyConstants.getUserRefreshIndexKey(userId);
        Set<String> members = redisTemplate.opsForSet().members(indexKey);
        if (members == null || members.isEmpty()) {
            return 0;
        }

        Set<String> tokens = new HashSet<>(members);
        tokens.remove(exceptToken);
        if (tokens.isEmpty()) {
            return 0;
        }

        // 索引里可能残留已自然过期的成员，删不存在的 key 是 no-op；
        // 计数以实际删除数为准，成员数会把这些残留算进去
        Long deleted = redisTemplate
            .delete(tokens.stream().map(RedisKeyConstants::getUserRefreshTokenKey).collect(Collectors.toSet()));
        redisTemplate.opsForSet().remove(indexKey, tokens.toArray());

        int revoked = deleted.intValue();
        log.info("撤销用户全部 Refresh Token userId={} count={}", userId, revoked);
        return revoked;
    }
}
