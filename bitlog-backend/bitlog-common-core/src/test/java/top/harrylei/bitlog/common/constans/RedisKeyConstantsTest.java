package top.harrylei.bitlog.common.constans;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;

@DisplayName("RedisKeyConstants Redis键构建测试")
class RedisKeyConstantsTest {

    @Test
    @DisplayName("常量前缀_全局前缀_符合预期格式")
    void globalPrefix_hasExpectedValue() {
        assertThat(RedisKeyConstants.GLOBAL_PREFIX).isEqualTo("bitlog:");
    }

    @Test
    @DisplayName("常量前缀_用户模块前缀_包含全局前缀")
    void userPrefix_containsGlobalPrefix() {
        assertThat(RedisKeyConstants.USER).startsWith(RedisKeyConstants.GLOBAL_PREFIX);
        assertThat(RedisKeyConstants.USER).isEqualTo("bitlog:user:");
    }

    @Test
    @DisplayName("常量前缀_锁模块前缀_包含全局前缀")
    void lockPrefix_containsGlobalPrefix() {
        assertThat(RedisKeyConstants.LOCK).startsWith(RedisKeyConstants.GLOBAL_PREFIX);
        assertThat(RedisKeyConstants.LOCK).isEqualTo("bitlog:lock:");
    }

    @Test
    @DisplayName("getUserRefreshTokenKey_正常tokenValue_返回正确格式的key")
    void getUserRefreshTokenKey_withValidTokenValue_returnsFormattedKey() {
        String tokenValue = "550e8400-e29b-41d4-a716-446655440000";

        String key = RedisKeyConstants.getUserRefreshTokenKey(tokenValue);

        assertThat(key).isEqualTo("bitlog:user:refresh:" + tokenValue);
        assertThat(key).startsWith(RedisKeyConstants.USER_REFRESH_TOKEN);
        assertThat(key).endsWith(tokenValue);
    }

    @Test
    @DisplayName("getUserInfoKey_正常userId_返回正确格式的key")
    void getUserInfoKey_withValidUserId_returnsFormattedKey() {
        Long userId = 456L;

        String key = RedisKeyConstants.getUserInfoKey(userId);

        assertThat(key).isEqualTo("bitlog:user:info:456");
        assertThat(key).startsWith(RedisKeyConstants.USER_INFO);
        assertThat(key).endsWith(String.valueOf(userId));
    }

    @Test
    @DisplayName("getDistributedLockKey_普通锁名_返回正确格式的key")
    void getDistributedLockKey_withLockName_returnsFormattedKey() {
        String lockName = "order:create:1001";

        String key = RedisKeyConstants.getDistributedLockKey(lockName);

        assertThat(key).isEqualTo("bitlog:lock:distributed:order:create:1001");
        assertThat(key).startsWith(RedisKeyConstants.DISTRIBUTED_LOCK);
        assertThat(key).endsWith(lockName);
    }

    @Test
    @DisplayName("getDuplicateLockKey_普通锁名_返回正确格式的key")
    void getDuplicateLockKey_withLockName_returnsFormattedKey() {
        String lockName = "user:register:harry";

        String key = RedisKeyConstants.getDuplicateLockKey(lockName);

        assertThat(key).isEqualTo("bitlog:lock:duplicate:user:register:harry");
        assertThat(key).startsWith(RedisKeyConstants.DUPLICATE_LOCK);
        assertThat(key).endsWith(lockName);
    }

    @Test
    @DisplayName("getUserRefreshTokenKey_不同tokenValue_返回不同的key")
    void getUserRefreshTokenKey_withDifferentTokenValues_returnsDifferentKeys() {
        String key1 = RedisKeyConstants.getUserRefreshTokenKey("token-aaa");
        String key2 = RedisKeyConstants.getUserRefreshTokenKey("token-bbb");

        assertThat(key1).isNotEqualTo(key2);
    }

    @Test
    @DisplayName("HEALTH_CHECK_常量_格式正确")
    void healthCheckConstant_hasExpectedFormat() {
        assertThat(RedisKeyConstants.HEALTH_CHECK).isEqualTo("bitlog:health:check");
        assertThat(RedisKeyConstants.HEALTH_CHECK).startsWith(RedisKeyConstants.GLOBAL_PREFIX);
    }

    @Test
    @DisplayName("USER_REFRESH_TOKEN前缀_格式正确")
    void userRefreshTokenPrefix_hasExpectedFormat() {
        assertThat(RedisKeyConstants.USER_REFRESH_TOKEN).isEqualTo("bitlog:user:refresh:");
    }

    @Test
    @DisplayName("USER_INFO前缀_格式正确")
    void userInfoPrefix_hasExpectedFormat() {
        assertThat(RedisKeyConstants.USER_INFO).isEqualTo("bitlog:user:info:");
    }
}
