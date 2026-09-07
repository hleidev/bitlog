package top.harrylei.bitlog.server;

import static org.assertj.core.api.Assertions.assertThat;

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
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.test.context.DynamicPropertyRegistry;
import org.springframework.test.context.DynamicPropertySource;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.transaction.annotation.Propagation;
import org.springframework.transaction.annotation.Transactional;
import org.testcontainers.containers.PostgreSQLContainer;
import org.testcontainers.junit.jupiter.Container;
import org.testcontainers.junit.jupiter.Testcontainers;
import top.harrylei.bitlog.auth.repository.dao.UserIdentityDAO;
import top.harrylei.bitlog.common.config.MybatisPlusConfig;
import top.harrylei.bitlog.file.service.FileService;
import top.harrylei.bitlog.file.util.FileUrlHelper;
import top.harrylei.bitlog.notification.listener.DeactivatedRecipientCleanupListener;
import top.harrylei.bitlog.notification.model.enums.NotificationTargetTypeEnum;
import top.harrylei.bitlog.notification.model.enums.NotificationTypeEnum;
import top.harrylei.bitlog.notification.repository.dao.NotificationDAO;
import top.harrylei.bitlog.notification.repository.entity.NotificationDO;
import top.harrylei.bitlog.notification.task.NotificationCleanupTask;
import top.harrylei.bitlog.user.converter.UserConverterImpl;
import top.harrylei.bitlog.user.repository.dao.UserDAO;
import top.harrylei.bitlog.user.repository.dao.UserInfoDAO;
import top.harrylei.bitlog.user.service.UserService;
import top.harrylei.bitlog.user.service.impl.UserServiceImpl;

/**
 * 通知清理集成测试
 *
 * @author Harry
 * @since 2026-09-07
 */
@Testcontainers
@MybatisPlusTest
@Import({
    MybatisPlusConfig.class,
    UserDAO.class,
    UserInfoDAO.class,
    UserIdentityDAO.class,
    UserConverterImpl.class,
    UserServiceImpl.class,
    NotificationDAO.class,
    DeactivatedRecipientCleanupListener.class,
    NotificationCleanupTask.class
})
@ImportAutoConfiguration(FlywayAutoConfiguration.class)
@Transactional(propagation = Propagation.NOT_SUPPORTED)
class NotificationCleanupIT {

    private static final long USER_A = 970L;
    private static final long USER_B = 971L;

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

    @MockitoBean
    private FileService fileService;

    @MockitoBean
    private FileUrlHelper fileUrlHelper;

    @Autowired
    private UserService userService;

    @Autowired
    private NotificationDAO notificationDAO;

    @Autowired
    private NotificationCleanupTask notificationCleanupTask;

    @Autowired
    private JdbcTemplate jdbc;

    @BeforeEach
    void seed() {
        jdbc.update("DELETE FROM notification");
        jdbc.update("DELETE FROM user_identity WHERE user_id IN (?, ?)", USER_A, USER_B);
        jdbc.update("DELETE FROM user_info WHERE user_id IN (?, ?)", USER_A, USER_B);
        jdbc.update("DELETE FROM user_account WHERE id IN (?, ?)", USER_A, USER_B);
        insertUser(USER_A, "cleanup-a");
        insertUser(USER_B, "cleanup-b");
    }

    private void insertUser(long id, String name) {
        jdbc.update(
                "INSERT INTO user_account (id, username, password, email, status, user_role, deleted) "
                        + "VALUES (?, ?, 'x', ?, 1, 0, 0)",
                id,
                name,
                name + "@example.com");
        jdbc.update("INSERT INTO user_info (user_id, avatar) VALUES (?, '')", id);
        jdbc.update(
                "INSERT INTO user_identity (user_id, provider, provider_user_id, provider_email) "
                        + "VALUES (?, 'google', ?, ?)",
                id,
                "g-" + id,
                name + "@example.com");
    }

    private Long insertNotification(long recipientId, Long actorId) {
        NotificationDO notification = new NotificationDO()
                .setRecipientId(recipientId)
                .setType(NotificationTypeEnum.COMMENT_REPLY)
                .setActorId(actorId)
                .setTargetType(NotificationTargetTypeEnum.COMMENT)
                .setTargetId(1L)
                .setPayload(Map.of());
        notificationDAO.save(notification);
        return notification.getId();
    }

    private Integer deleted(long notificationId) {
        return jdbc.queryForObject("SELECT deleted FROM notification WHERE id = ?", Integer.class, notificationId);
    }

    private Integer notificationCount(long notificationId) {
        return jdbc.queryForObject("SELECT count(*) FROM notification WHERE id = ?", Integer.class, notificationId);
    }

    @Test
    @DisplayName("deactivateUserBatch_注销用户_其收件通知被软删")
    void deactivateUserBatch_deactivatedRecipient_receivedNotificationsSoftDeleted() {
        Long notificationId = insertNotification(USER_A, USER_B);

        userService.deactivateUserBatch(List.of(USER_A));

        assertThat(deleted(notificationId)).isOne();
    }

    @Test
    @DisplayName("deactivateUserBatch_注销用户_其作为actor的通知保留")
    void deactivateUserBatch_deactivatedActor_actorNotificationsRetained() {
        Long notificationId = insertNotification(USER_B, USER_A);

        userService.deactivateUserBatch(List.of(USER_A));

        assertThat(deleted(notificationId)).isZero();
    }

    @Test
    @DisplayName("cleanExpiredNotifications_已读且超期_被物理删除")
    void cleanExpiredNotifications_readAndExpired_physicallyDeleted() {
        Long notificationId = insertNotification(USER_B, USER_A);
        jdbc.update(
                "UPDATE notification SET create_time = now() - INTERVAL '100 days', read_time = now() WHERE id = ?",
                notificationId);

        notificationCleanupTask.cleanExpiredNotifications();

        assertThat(notificationCount(notificationId)).isZero();
    }

    @Test
    @DisplayName("cleanExpiredNotifications_未读且超期_保留")
    void cleanExpiredNotifications_unreadAndExpired_retained() {
        Long notificationId = insertNotification(USER_B, USER_A);
        jdbc.update(
                "UPDATE notification SET create_time = now() - INTERVAL '100 days', read_time = NULL, deleted = 0 "
                        + "WHERE id = ?",
                notificationId);

        notificationCleanupTask.cleanExpiredNotifications();

        assertThat(notificationCount(notificationId)).isOne();
    }

    @Test
    @DisplayName("cleanExpiredNotifications_已软删且超期_被物理删除")
    void cleanExpiredNotifications_softDeletedAndExpired_physicallyDeleted() {
        Long notificationId = insertNotification(USER_B, USER_A);
        jdbc.update(
                "UPDATE notification SET create_time = now() - INTERVAL '100 days', read_time = NULL, deleted = 1 "
                        + "WHERE id = ?",
                notificationId);

        notificationCleanupTask.cleanExpiredNotifications();

        assertThat(notificationCount(notificationId)).isZero();
    }
}
