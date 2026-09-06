package top.harrylei.bitlog.server;

import com.baomidou.mybatisplus.test.autoconfigure.MybatisPlusTest;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.autoconfigure.ImportAutoConfiguration;
import org.springframework.boot.autoconfigure.flyway.FlywayAutoConfiguration;
import org.springframework.context.annotation.Import;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.test.context.DynamicPropertyRegistry;
import org.springframework.test.context.DynamicPropertySource;
import org.testcontainers.containers.PostgreSQLContainer;
import org.testcontainers.junit.jupiter.Container;
import org.testcontainers.junit.jupiter.Testcontainers;
import top.harrylei.bitlog.common.config.MybatisPlusConfig;
import top.harrylei.bitlog.notification.model.enums.NotificationTargetTypeEnum;
import top.harrylei.bitlog.notification.model.enums.NotificationTypeEnum;
import top.harrylei.bitlog.notification.model.dto.NotificationCreateDTO;
import top.harrylei.bitlog.notification.port.NotificationPort;
import top.harrylei.bitlog.notification.port.impl.NotificationPortImpl;
import top.harrylei.bitlog.notification.repository.dao.NotificationDAO;
import top.harrylei.bitlog.notification.repository.entity.NotificationDO;
import top.harrylei.bitlog.notification.service.impl.NotificationServiceImpl;

import java.util.List;
import java.util.Map;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatCode;

/**
 * 通知派发幂等性集成测试
 * <p>
 * dispatch 落库走 {@code INSERT ... ON CONFLICT DO NOTHING}，而不是捕获唯一约束异常，本测试钉住的 正是这一点：重复的候选不会让所在事务被标记为
 * aborted，同一事务里后续的写入依然能成功。
 * </p>
 *
 * @author Harry
 * @since 2026-09-06
 */
@Testcontainers
@MybatisPlusTest
@Import({MybatisPlusConfig.class, NotificationDAO.class, NotificationServiceImpl.class, NotificationPortImpl.class})
@ImportAutoConfiguration(FlywayAutoConfiguration.class)
class NotificationDispatchIT {

    private static final long RECIPIENT = 930L;
    private static final long ACTOR = 931L;

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
        registry.add("mybatis-plus.configuration.default-enum-type-handler",
            () -> "com.baomidou.mybatisplus.core.handlers.MybatisEnumTypeHandler");
        registry.add("mybatis-plus.mapper-locations", () -> "classpath*:mapper/*.xml");
    }

    @Autowired
    private NotificationPort notificationPort;

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

    private NotificationCreateDTO newCommand(Long targetId) {
        return new NotificationCreateDTO(RECIPIENT, NotificationTypeEnum.COMMENT_REPLY, ACTOR,
            NotificationTargetTypeEnum.COMMENT, targetId, Map.of());
    }

    @Test
    @DisplayName("dispatch_同一条command连续派发两次_表里只有一行且第二次不抛异常")
    void dispatch_sameCommandTwice_onlyOneRowAndNoExceptionOnSecond() {
        NotificationCreateDTO command = newCommand(1L);

        notificationPort.dispatch(List.of(command));

        assertThatCode(() -> notificationPort.dispatch(List.of(command))).doesNotThrowAnyException();

        List<NotificationDO> all = notificationDAO.lambdaQuery().eq(NotificationDO::getRecipientId, RECIPIENT).list();
        assertThat(all).hasSize(1);
    }

    @Test
    @DisplayName("dispatch_同一事务内先重复后全新_全新的一条仍能成功落库")
    void dispatch_duplicateThenNewInSameTransaction_newOneStillPersists() {
        NotificationCreateDTO duplicate = newCommand(2L);
        notificationPort.dispatch(List.of(duplicate));

        // 重复派发不应把当前事务标记为 aborted，紧随其后的全新写入必须依然成功
        notificationPort.dispatch(List.of(duplicate));
        NotificationCreateDTO fresh = newCommand(3L);
        notificationPort.dispatch(List.of(fresh));

        List<NotificationDO> all = notificationDAO.lambdaQuery().eq(NotificationDO::getRecipientId, RECIPIENT).list();
        assertThat(all).extracting(NotificationDO::getTargetId).containsExactlyInAnyOrder(2L, 3L);
    }
}
