package top.harrylei.bitlog.server;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyInt;
import static org.mockito.Mockito.when;

import com.baomidou.mybatisplus.test.autoconfigure.MybatisPlusTest;
import java.util.List;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.autoconfigure.ImportAutoConfiguration;
import org.springframework.boot.autoconfigure.flyway.FlywayAutoConfiguration;
import org.springframework.boot.context.properties.EnableConfigurationProperties;
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
import top.harrylei.bitlog.common.config.MybatisPlusConfig;
import top.harrylei.bitlog.common.context.ReqInfoContext;
import top.harrylei.bitlog.common.util.RateLimiter;
import top.harrylei.bitlog.file.service.FileService;
import top.harrylei.bitlog.file.util.FileUrlHelper;
import top.harrylei.bitlog.link.config.FriendLinkProperties;
import top.harrylei.bitlog.link.converter.FriendLinkConverterImpl;
import top.harrylei.bitlog.link.model.enums.FriendLinkStatusEnum;
import top.harrylei.bitlog.link.model.req.FriendLinkAuditParam;
import top.harrylei.bitlog.link.model.req.FriendLinkSaveParam;
import top.harrylei.bitlog.link.repository.dao.FriendLinkDAO;
import top.harrylei.bitlog.link.service.FriendLinkService;
import top.harrylei.bitlog.link.service.impl.FriendLinkServiceImpl;
import top.harrylei.bitlog.notification.converter.NotificationConverterImpl;
import top.harrylei.bitlog.notification.listener.FriendLinkNotificationListener;
import top.harrylei.bitlog.notification.model.enums.NotificationTypeEnum;
import top.harrylei.bitlog.notification.port.impl.NotificationPortImpl;
import top.harrylei.bitlog.notification.repository.dao.NotificationDAO;
import top.harrylei.bitlog.notification.repository.entity.NotificationDO;
import top.harrylei.bitlog.notification.service.impl.NotificationServiceImpl;
import top.harrylei.bitlog.user.converter.UserConverterImpl;
import top.harrylei.bitlog.user.port.impl.UserPortImpl;
import top.harrylei.bitlog.user.repository.dao.UserDAO;
import top.harrylei.bitlog.user.repository.dao.UserInfoDAO;

/**
 * 友链申请与审核通知集成测试
 *
 * @author Harry
 * @since 2026-09-07
 */
@Testcontainers
@MybatisPlusTest
@Import({
    MybatisPlusConfig.class,
    FriendLinkDAO.class,
    FriendLinkConverterImpl.class,
    FriendLinkServiceImpl.class,
    NotificationDAO.class,
    NotificationConverterImpl.class,
    NotificationServiceImpl.class,
    NotificationPortImpl.class,
    FriendLinkNotificationListener.class,
    UserDAO.class,
    UserInfoDAO.class,
    UserConverterImpl.class,
    UserPortImpl.class
})
@EnableConfigurationProperties(FriendLinkProperties.class)
@ImportAutoConfiguration(FlywayAutoConfiguration.class)
@Transactional(propagation = Propagation.NOT_SUPPORTED)
class FriendLinkNotificationIT {

    private static final long APPLICANT = 970L;
    private static final long ADMIN_A = 971L;
    private static final long ADMIN_B = 972L;

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
    private RateLimiter rateLimiter;

    @MockitoBean
    private FileService fileService;

    @MockitoBean
    private FileUrlHelper fileUrlHelper;

    @Autowired
    private FriendLinkService friendLinkService;

    @Autowired
    private NotificationDAO notificationDAO;

    @Autowired
    private JdbcTemplate jdbc;

    @BeforeEach
    void seed() {
        when(rateLimiter.tryAcquire(any(), anyInt(), any())).thenReturn(RateLimiter.Result.allow());
        ReqInfoContext.setContext(new ReqInfoContext.ReqInfo().setUserId(ADMIN_A));

        jdbc.update("DELETE FROM notification");
        jdbc.update("DELETE FROM friend_link");
        jdbc.update("DELETE FROM user_account WHERE id IN (?, ?, ?)", APPLICANT, ADMIN_A, ADMIN_B);
        insertUser(APPLICANT, "link-applicant", 0);
        insertUser(ADMIN_A, "link-admin-a", 1);
        insertUser(ADMIN_B, "link-admin-b", 1);
    }

    @AfterEach
    void clearContext() {
        ReqInfoContext.clear();
    }

    private void insertUser(long id, String username, int role) {
        jdbc.update(
                "INSERT INTO user_account (id, username, password, email, status, user_role, deleted) "
                        + "VALUES (?, ?, 'x', ?, 1, ?, 0)",
                id,
                username,
                username + "@example.com",
                role);
    }

    private FriendLinkSaveParam applyParam(String url) {
        FriendLinkSaveParam param = new FriendLinkSaveParam();
        param.setName("BitLog 友站");
        param.setUrl(url);
        param.setApplyMessage("申请加入友链");
        return param;
    }

    private FriendLinkAuditParam auditParam(FriendLinkStatusEnum status, String rejectReason) {
        FriendLinkAuditParam param = new FriendLinkAuditParam();
        param.setStatus(status);
        param.setRejectReason(rejectReason);
        return param;
    }

    private List<NotificationDO> findByRecipientAndType(long recipientId, NotificationTypeEnum type) {
        return notificationDAO
                .lambdaQuery()
                .eq(NotificationDO::getRecipientId, recipientId)
                .eq(NotificationDO::getType, type)
                .orderByAsc(NotificationDO::getId)
                .list();
    }

    @Test
    @DisplayName("applyMine_存在两个管理员_每个管理员各落一条申请通知")
    void applyMine_twoAdmins_eachAdminGetsOneAppliedNotification() {
        Long linkId = friendLinkService.applyMine(APPLICANT, applyParam("https://two-admins.example.com"));

        List<NotificationDO> adminANotifications = findByRecipientAndType(ADMIN_A, NotificationTypeEnum.LINK_APPLIED);
        List<NotificationDO> adminBNotifications = findByRecipientAndType(ADMIN_B, NotificationTypeEnum.LINK_APPLIED);
        assertThat(adminANotifications).singleElement().satisfies(notification -> {
            assertThat(notification.getActorId()).isEqualTo(APPLICANT);
            assertThat(notification.getTargetId()).isEqualTo(linkId);
            assertThat(notification.getDedupeKey()).isEqualTo("link_applied:" + linkId);
        });
        assertThat(adminBNotifications).singleElement().satisfies(notification -> {
            assertThat(notification.getActorId()).isEqualTo(APPLICANT);
            assertThat(notification.getTargetId()).isEqualTo(linkId);
            assertThat(notification.getDedupeKey()).isEqualTo("link_applied:" + linkId);
        });
    }

    @Test
    @DisplayName("applyMine_申请人本人是管理员_不给自己落通知")
    void applyMine_applicantIsAdmin_noNotificationForApplicant() {
        jdbc.update("UPDATE user_account SET user_role = 1 WHERE id = ?", APPLICANT);

        friendLinkService.applyMine(APPLICANT, applyParam("https://self-admin.example.com"));

        assertThat(findByRecipientAndType(APPLICANT, NotificationTypeEnum.LINK_APPLIED))
                .isEmpty();
        assertThat(findByRecipientAndType(ADMIN_A, NotificationTypeEnum.LINK_APPLIED))
                .hasSize(1);
    }

    @Test
    @DisplayName("applyMine_禁用的管理员_不落通知")
    void applyMine_disabledAdmin_getsNoNotification() {
        jdbc.update("UPDATE user_account SET status = 0 WHERE id = ?", ADMIN_B);

        friendLinkService.applyMine(APPLICANT, applyParam("https://disabled-admin.example.com"));

        assertThat(findByRecipientAndType(ADMIN_A, NotificationTypeEnum.LINK_APPLIED))
                .hasSize(1);
        assertThat(findByRecipientAndType(ADMIN_B, NotificationTypeEnum.LINK_APPLIED))
                .isEmpty();
    }

    @Test
    @DisplayName("audit_先拒绝后通过_申请人收到两条审核通知")
    void audit_rejectedThenApproved_applicantGetsTwoReviewedNotifications() {
        Long linkId = friendLinkService.applyMine(APPLICANT, applyParam("https://review-twice.example.com"));

        friendLinkService.audit(linkId, auditParam(FriendLinkStatusEnum.REJECTED, "未找到回链"));
        friendLinkService.audit(linkId, auditParam(FriendLinkStatusEnum.APPROVED, null));

        List<NotificationDO> notifications = findByRecipientAndType(APPLICANT, NotificationTypeEnum.LINK_REVIEWED);
        assertThat(notifications)
                .hasSize(2)
                .extracting(NotificationDO::getDedupeKey)
                .containsOnlyNulls();
        assertThat(notifications)
                .extracting(notification -> notification.getPayload().get("status"))
                .containsExactly(FriendLinkStatusEnum.REJECTED.getCode(), FriendLinkStatusEnum.APPROVED.getCode());
    }
}
