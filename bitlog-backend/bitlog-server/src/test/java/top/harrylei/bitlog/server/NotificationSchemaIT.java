package top.harrylei.bitlog.server;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import com.baomidou.mybatisplus.test.autoconfigure.MybatisPlusTest;
import java.util.List;
import java.util.Map;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.autoconfigure.ImportAutoConfiguration;
import org.springframework.boot.autoconfigure.flyway.FlywayAutoConfiguration;
import org.springframework.context.annotation.Import;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.test.context.DynamicPropertyRegistry;
import org.springframework.test.context.DynamicPropertySource;
import org.testcontainers.containers.PostgreSQLContainer;
import org.testcontainers.junit.jupiter.Container;
import org.testcontainers.junit.jupiter.Testcontainers;
import top.harrylei.bitlog.common.config.MybatisPlusConfig;
import top.harrylei.bitlog.notification.model.enums.NotificationTargetTypeEnum;
import top.harrylei.bitlog.notification.model.enums.NotificationTypeEnum;
import top.harrylei.bitlog.notification.repository.dao.NotificationDAO;
import top.harrylei.bitlog.notification.repository.entity.NotificationDO;

/**
 * 通知表结构与约束集成测试
 * <p>
 * payload 是 jsonb 列，PG 驱动对 setString 参数绑定 jsonb 目标列会拒绝，这里必须真的做一次插入 - 读回的往返，才能钉住 JacksonTypeHandler 落库不是静默写坏或直接抛异常。
 * </p>
 *
 * @author Harry
 * @since 2026-09-06
 */
@Testcontainers
@MybatisPlusTest
@Import({MybatisPlusConfig.class, NotificationDAO.class})
@ImportAutoConfiguration(FlywayAutoConfiguration.class)
class NotificationSchemaIT {

    private static final long RECIPIENT = 920L;
    private static final long ACTOR = 921L;

    @Container
    @SuppressWarnings("resource")
    static final PostgreSQLContainer<?> POSTGRES =
            new PostgreSQLContainer<>("postgres:16-alpine").withDatabaseName("bitlog");

    @DynamicPropertySource
    static void datasource(DynamicPropertyRegistry registry) {
        registry.add("spring.datasource.url", POSTGRES::getJdbcUrl);
        registry.add("spring.datasource.username", POSTGRES::getUsername);
        registry.add("spring.datasource.password", POSTGRES::getPassword);
        registry.add("spring.flyway.enabled", () -> true);
        registry.add("spring.flyway.locations", () -> "classpath:db/migration");
        registry.add(
                "mybatis-plus.configuration.default-enum-type-handler",
                () -> "com.baomidou.mybatisplus.core.handlers.MybatisEnumTypeHandler");
        registry.add("mybatis-plus.mapper-locations", () -> "classpath*:mapper/*.xml");
    }

    @Autowired
    private NotificationDAO notificationDAO;

    @Autowired
    private JdbcTemplate jdbc;

    @BeforeEach
    void seed() {
        jdbc.update("DELETE FROM notification");
        jdbc.update("""
            INSERT INTO user_account (id, username, email) VALUES (?, ?, ?)
            ON CONFLICT (id) DO NOTHING
            """, RECIPIENT, "recipient-" + RECIPIENT, "recipient-" + RECIPIENT + "@test.local");
        jdbc.update("""
            INSERT INTO user_account (id, username, email) VALUES (?, ?, ?)
            ON CONFLICT (id) DO NOTHING
            """, ACTOR, "actor-" + ACTOR, "actor-" + ACTOR + "@test.local");
    }

    private NotificationDO newNotification(Long targetId) {
        return new NotificationDO()
                .setRecipientId(RECIPIENT)
                .setType(NotificationTypeEnum.COMMENT_REPLY)
                .setActorId(ACTOR)
                .setTargetType(NotificationTargetTypeEnum.COMMENT)
                .setTargetId(targetId);
    }

    @Test
    @DisplayName("flywayMigrate_运行V4_通知表建出并可写入")
    void flywayMigrate_runsV4_tableIsUsable() {
        NotificationDO notification = newNotification(1L);
        notification.setPayload(Map.of());

        notificationDAO.save(notification);

        assertThat(notification.getId()).isNotNull();
        assertThat(notificationDAO.getById(notification.getId())).isNotNull();
    }

    @Test
    @DisplayName("save_写入非空payload_读回内容完整还原")
    void save_nonEmptyPayload_roundTripsIntact() {
        NotificationDO notification = newNotification(2L);
        Map<String, Object> payload = Map.of("commentId", 2, "excerpt", "这是一段评论摘要", "articleTitle", "文章标题");
        notification.setPayload(payload);

        notificationDAO.save(notification);

        NotificationDO reloaded = notificationDAO.getById(notification.getId());
        assertThat(reloaded.getPayload()).isEqualTo(payload);
    }

    @Test
    @DisplayName("save_同一收件人同类型同目标_第二条违反唯一约束")
    void save_duplicateRecipientTypeTarget_violatesUniqueConstraint() {
        Long targetId = 3L;
        notificationDAO.save(newNotification(targetId).setPayload(Map.of()));

        NotificationDO duplicate = newNotification(targetId).setPayload(Map.of());
        assertThatThrownBy(() -> notificationDAO.save(duplicate)).isInstanceOf(DataIntegrityViolationException.class);
    }

    @Test
    @DisplayName("save_targetId为NULL_不受去重唯一约束限制_可插入多条")
    void save_nullTargetId_notConstrainedByDedupeIndex() {
        notificationDAO.save(newNotification(null).setPayload(Map.of()));
        notificationDAO.save(newNotification(null).setPayload(Map.of()));

        List<NotificationDO> all = notificationDAO
                .lambdaQuery()
                .eq(NotificationDO::getRecipientId, RECIPIENT)
                .list();
        assertThat(all).hasSize(2);
    }
}
