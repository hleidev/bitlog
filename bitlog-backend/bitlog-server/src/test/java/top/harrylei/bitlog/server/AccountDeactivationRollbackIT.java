package top.harrylei.bitlog.server;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.autoconfigure.ImportAutoConfiguration;
import org.springframework.boot.autoconfigure.flyway.FlywayAutoConfiguration;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.context.annotation.Import;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.test.context.DynamicPropertyRegistry;
import org.springframework.test.context.DynamicPropertySource;
import org.springframework.transaction.annotation.Propagation;
import org.springframework.transaction.annotation.Transactional;
import org.testcontainers.containers.PostgreSQLContainer;
import org.testcontainers.junit.jupiter.Container;
import org.testcontainers.junit.jupiter.Testcontainers;
import top.harrylei.bitlog.auth.listener.AccountCleanupListener;
import top.harrylei.bitlog.auth.repository.dao.UserIdentityDAO;
import top.harrylei.bitlog.auth.support.RefreshTokenStore;
import top.harrylei.bitlog.common.config.MybatisPlusConfig;
import top.harrylei.bitlog.file.util.FileUrlHelper;
import top.harrylei.bitlog.file.service.FileService;
import top.harrylei.bitlog.user.converter.UserConverterImpl;
import top.harrylei.bitlog.user.repository.dao.UserDAO;
import top.harrylei.bitlog.user.repository.dao.UserInfoDAO;
import top.harrylei.bitlog.user.service.UserService;
import top.harrylei.bitlog.user.service.impl.UserServiceImpl;

import com.baomidou.mybatisplus.test.autoconfigure.MybatisPlusTest;

import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.doThrow;

/**
 * 注销清理的事务一致性集成测试
 * <p>
 * 注销的第三方解绑与会话撤销发生在认证侧的 {@link AccountCleanupListener} 里，靠同步 {@code @EventListener}
 * 与发布方共享事务。本测试钉住这一点：监听器抛错时，先前已完成的墓碑写入与解绑必须一并回滚。 若哪天有人把它换成 {@code @TransactionalEventListener}，这里会失败。
 * </p>
 *
 * @author Harry
 * @since 2026-08-09
 */
@Testcontainers
@MybatisPlusTest
@Import({MybatisPlusConfig.class, UserDAO.class, UserInfoDAO.class, UserIdentityDAO.class, UserConverterImpl.class,
    UserServiceImpl.class, AccountCleanupListener.class})
@ImportAutoConfiguration(FlywayAutoConfiguration.class)
// MybatisPlusTest 默认把测试方法包进事务，那会让被测方法的回滚无从观察，故显式退出
@Transactional(propagation = Propagation.NOT_SUPPORTED)
class AccountDeactivationRollbackIT {

    private static final long KEEPER = 901L;
    private static final long BREAKER = 902L;

    @Container
    @SuppressWarnings("resource")
    static final PostgreSQLContainer<?> POSTGRES = new PostgreSQLContainer<>("postgres:16-alpine").withDatabaseName("bitlog");

    @DynamicPropertySource
    static void datasource(DynamicPropertyRegistry registry) {
        registry.add("spring.datasource.url", POSTGRES::getJdbcUrl);
        registry.add("spring.datasource.username", POSTGRES::getUsername);
        registry.add("spring.datasource.password", POSTGRES::getPassword);
        registry.add("spring.flyway.enabled", () -> true);
        registry.add("spring.flyway.locations", () -> "classpath:db/migration");
        registry.add("mybatis-plus.configuration.default-enum-type-handler",
            () -> "com.baomidou.mybatisplus.core.handlers.MybatisEnumTypeHandler");
        registry.add("mybatis-plus.mapper-locations", () -> "classpath*:mapper/*.xml");
    }

    /** 撤销会话走 Redis，与本测试的断言无关；同时它是注入失败的抓手 */
    @MockitoBean
    private RefreshTokenStore refreshTokenStore;

    /** 头像回收是提交后的外部 IO，种子数据留空头像即不会触发 */
    @MockitoBean
    private FileService fileService;

    @MockitoBean
    private FileUrlHelper fileUrlHelper;

    @Autowired
    private UserService userService;

    @Autowired
    private JdbcTemplate jdbc;

    @BeforeEach
    void seed() {
        jdbc.update("DELETE FROM user_identity WHERE user_id IN (?, ?)", KEEPER, BREAKER);
        jdbc.update("DELETE FROM user_info WHERE user_id IN (?, ?)", KEEPER, BREAKER);
        jdbc.update("DELETE FROM user_account WHERE id IN (?, ?)", KEEPER, BREAKER);
        insert(KEEPER, "keeper");
        insert(BREAKER, "breaker");
    }

    private void insert(long id, String name) {
        jdbc.update("INSERT INTO user_account (id, username, password, email, status, user_role, deleted) "
            + "VALUES (?, ?, 'x', ?, 1, 0, 0)", id, name, name + "@example.com");
        jdbc.update("INSERT INTO user_info (user_id, avatar) VALUES (?, '')", id);
        jdbc.update("INSERT INTO user_identity (user_id, provider, provider_user_id, provider_email) "
            + "VALUES (?, 'google', ?, ?)", id, "g-" + id, name + "@example.com");
    }

    @Test
    @DisplayName("deactivateUserBatch_监听器抛错_先前用户的墓碑与解绑一并回滚")
    void deactivateUserBatch_listenerFails_rollsBackEarlierUser() {
        doThrow(new IllegalStateException("撤销会话失败")).when(refreshTokenStore).revokeAll(eq(BREAKER), any());

        assertThatThrownBy(() -> userService.deactivateUserBatch(List.of(KEEPER, BREAKER)))
            .isInstanceOf(IllegalStateException.class);

        assertThat(username(KEEPER)).as("墓碑写入应回滚").isEqualTo("keeper");
        assertThat(deleted(KEEPER)).as("注销标记应回滚").isZero();
        assertThat(identityCount(KEEPER)).as("第三方解绑应回滚").isOne();
    }

    @Test
    @DisplayName("deactivateUserBatch_全部成功_墓碑与解绑均已提交")
    void deactivateUserBatch_allSucceed_commits() {
        userService.deactivateUserBatch(List.of(KEEPER, BREAKER));

        assertThat(username(KEEPER)).as("用户名应被墓碑值覆写").isNotEqualTo("keeper");
        assertThat(deleted(KEEPER)).isOne();
        assertThat(identityCount(KEEPER)).as("第三方身份应被物理删除").isZero();
        assertThat(identityCount(BREAKER)).isZero();
    }

    private String username(long userId) {
        return jdbc.queryForObject("SELECT username FROM user_account WHERE id = ?", String.class, userId);
    }

    private Integer deleted(long userId) {
        return jdbc.queryForObject("SELECT deleted FROM user_account WHERE id = ?", Integer.class, userId);
    }

    private Integer identityCount(long userId) {
        return jdbc.queryForObject("SELECT COUNT(*) FROM user_identity WHERE user_id = ?", Integer.class, userId);
    }
}
