package top.harrylei.bitlog.server;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatCode;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.anyList;
import static org.mockito.Mockito.when;

import com.baomidou.mybatisplus.test.autoconfigure.MybatisPlusTest;
import java.time.OffsetDateTime;
import java.util.List;
import java.util.Map;
import java.util.concurrent.atomic.AtomicLong;
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
import org.testcontainers.containers.PostgreSQLContainer;
import org.testcontainers.junit.jupiter.Container;
import org.testcontainers.junit.jupiter.Testcontainers;
import top.harrylei.bitlog.common.config.MybatisPlusConfig;
import top.harrylei.bitlog.common.enums.ResultCode;
import top.harrylei.bitlog.common.exception.BusinessException;
import top.harrylei.bitlog.common.model.PageVO;
import top.harrylei.bitlog.notification.converter.NotificationConverterImpl;
import top.harrylei.bitlog.notification.model.enums.NotificationTargetTypeEnum;
import top.harrylei.bitlog.notification.model.enums.NotificationTypeEnum;
import top.harrylei.bitlog.notification.model.query.NotificationPageParam;
import top.harrylei.bitlog.notification.model.vo.NotificationVO;
import top.harrylei.bitlog.notification.repository.dao.NotificationDAO;
import top.harrylei.bitlog.notification.repository.entity.NotificationDO;
import top.harrylei.bitlog.notification.service.impl.NotificationServiceImpl;
import top.harrylei.bitlog.user.model.vo.UserVO;
import top.harrylei.bitlog.user.port.UserPort;

/**
 * 通知查询与已读标记集成测试
 * <p>
 * 覆盖点集中在越权防护：分页、未读数、单条已读都必须以收件人为过滤条件， 标记他人的通知为已读必须原样失败且不改动数据。
 * </p>
 *
 * @author Harry
 * @since 2026-09-07
 */
@Testcontainers
@MybatisPlusTest
@Import({MybatisPlusConfig.class, NotificationDAO.class, NotificationConverterImpl.class, NotificationServiceImpl.class
})
@ImportAutoConfiguration(FlywayAutoConfiguration.class)
class NotificationQueryIT {

    private static final long USER_A = 950L;
    private static final long USER_B = 951L;
    private static final long ACTOR = 952L;

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

    /** 查询用不到派发逻辑，仅用于满足构造注入，各用例按需 stub */
    @MockitoBean
    private UserPort userPort;

    @Autowired
    private NotificationServiceImpl notificationService;

    @Autowired
    private NotificationDAO notificationDAO;

    @Autowired
    private JdbcTemplate jdbc;

    @BeforeEach
    void seed() {
        jdbc.update("DELETE FROM notification");
        insertUser(USER_A, "usera");
        insertUser(USER_B, "userb");
        insertUser(ACTOR, "actor");
    }

    private void insertUser(long id, String name) {
        jdbc.update(
                "INSERT INTO user_account (id, username, password, email, status, user_role, deleted) "
                        + "VALUES (?, ?, 'x', ?, 1, 0, 0) ON CONFLICT (id) DO NOTHING",
                id,
                name,
                name + "@test.local");
    }

    /** 查询测试通知不参与去重，每条通知使用不同 targetId 便于区分 */
    private final AtomicLong nextTargetId = new AtomicLong(1);

    private Long insertNotification(long recipientId, Long actorId, boolean read) {
        NotificationDO notification = new NotificationDO()
                .setRecipientId(recipientId)
                .setType(NotificationTypeEnum.COMMENT_REPLY)
                .setActorId(actorId)
                .setTargetType(NotificationTargetTypeEnum.COMMENT)
                .setTargetId(nextTargetId.getAndIncrement())
                .setPayload(Map.of());
        notificationDAO.save(notification);
        if (read) {
            jdbc.update("UPDATE notification SET read_time = now() WHERE id = ?", notification.getId());
        }
        return notification.getId();
    }

    private NotificationPageParam pageParam() {
        return new NotificationPageParam();
    }

    @Test
    @DisplayName("pageNotifications_混有他人通知_只返回当前用户的")
    void pageNotifications_mixedWithOthers_onlyReturnsCurrentUsersNotifications() {
        Long mine1 = insertNotification(USER_A, ACTOR, false);
        Long mine2 = insertNotification(USER_A, ACTOR, false);
        insertNotification(USER_B, ACTOR, false);

        PageVO<NotificationVO> page = notificationService.pageNotifications(USER_A, pageParam());

        assertThat(page.getTotalElements()).isEqualTo(2);
        assertThat(page.getContent()).extracting(NotificationVO::getId).containsExactlyInAnyOrder(mine1, mine2);
    }

    @Test
    @DisplayName("countUnread_混有他人未读_只统计当前用户的未读")
    void countUnread_mixedWithOthers_onlyCountsCurrentUsersUnread() {
        insertNotification(USER_A, ACTOR, false);
        insertNotification(USER_A, ACTOR, true);
        insertNotification(USER_B, ACTOR, false);
        insertNotification(USER_B, ACTOR, false);

        assertThat(notificationService.countUnread(USER_A)).isEqualTo(1);
        assertThat(notificationService.countUnread(USER_B)).isEqualTo(2);
    }

    @Test
    @DisplayName("markRead_标记自己的未读通知_已读时刻写入且未读数减一")
    void markRead_ownUnreadNotification_readTimeSetAndUnreadCountDecreases() {
        Long id = insertNotification(USER_A, ACTOR, false);
        insertNotification(USER_A, ACTOR, false);

        notificationService.markRead(USER_A, id);

        NotificationDO updated = notificationDAO.getById(id);
        assertThat(updated.getReadTime()).isNotNull();
        assertThat(notificationService.countUnread(USER_A)).isEqualTo(1);
    }

    @Test
    @DisplayName("markRead_对同一条已读通知重复标记_不抛异常且read_time保持首次的值")
    void markRead_calledTwiceOnSameNotification_secondCallIsNoopAndKeepsFirstReadTime() {
        Long id = insertNotification(USER_A, ACTOR, false);

        notificationService.markRead(USER_A, id);
        OffsetDateTime firstReadTime = notificationDAO.getById(id).getReadTime();

        assertThatCode(() -> notificationService.markRead(USER_A, id)).doesNotThrowAnyException();
        OffsetDateTime secondReadTime = notificationDAO.getById(id).getReadTime();
        assertThat(secondReadTime).isEqualTo(firstReadTime);
    }

    @Test
    @DisplayName("markRead_标记他人的通知_抛NOTIFICATION_NOT_EXISTS且那条通知仍未读")
    void markRead_othersNotification_throwsNotExistsAndStaysUnread() {
        Long othersId = insertNotification(USER_B, ACTOR, false);

        assertThatThrownBy(() -> notificationService.markRead(USER_A, othersId))
                .isInstanceOf(BusinessException.class)
                .extracting(ex -> ((BusinessException) ex).getCode())
                .isEqualTo(ResultCode.NOTIFICATION_NOT_EXISTS.getCode());

        // 越权防护的关键断言：SQL 未命中就不该有任何行被改动，重新查库确认 read_time 仍是 null
        NotificationDO stillUnread = notificationDAO.getById(othersId);
        assertThat(stillUnread.getReadTime()).isNull();
        assertThat(notificationService.countUnread(USER_B)).isEqualTo(1);
    }

    @Test
    @DisplayName("markAllRead_标记当前用户全部已读_未读数归零且不影响他人")
    void markAllRead_currentUser_ownUnreadCountZeroAndOthersUnaffected() {
        insertNotification(USER_A, ACTOR, false);
        insertNotification(USER_A, ACTOR, false);
        insertNotification(USER_B, ACTOR, false);

        notificationService.markAllRead(USER_A);

        assertThat(notificationService.countUnread(USER_A)).isZero();
        assertThat(notificationService.countUnread(USER_B)).isEqualTo(1);
    }

    @Test
    @DisplayName("pageNotifications_通知带触发者_actor信息被正确填充")
    void pageNotifications_withActor_actorFieldsPopulated() {
        insertNotification(USER_A, ACTOR, false);
        UserVO actorVO = new UserVO();
        actorVO.setUserId(ACTOR);
        actorVO.setUsername("actor");
        actorVO.setAvatar("avatar.png");
        actorVO.setDeactivated(false);
        actorVO.setEmail("actor@test.local");
        when(userPort.getUserBatchByIds(anyList())).thenReturn(List.of(actorVO));

        PageVO<NotificationVO> page = notificationService.pageNotifications(USER_A, pageParam());

        assertThat(page.getContent()).hasSize(1);
        NotificationVO vo = page.getContent().get(0);
        assertThat(vo.getActor().getUserId()).isEqualTo(ACTOR);
        assertThat(vo.getActor().getUsername()).isEqualTo("actor");
        assertThat(vo.getActor().getAvatar()).isEqualTo("avatar.png");
        assertThat(vo.getActor().getDeactivated()).isFalse();
    }
}
