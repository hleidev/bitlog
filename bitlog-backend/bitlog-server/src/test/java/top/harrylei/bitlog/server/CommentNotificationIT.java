package top.harrylei.bitlog.server;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyInt;
import static org.mockito.ArgumentMatchers.anyList;
import static org.mockito.Mockito.doThrow;
import static org.mockito.Mockito.when;

import com.baomidou.mybatisplus.test.autoconfigure.MybatisPlusTest;
import java.util.List;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.mockito.Mockito;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.autoconfigure.ImportAutoConfiguration;
import org.springframework.boot.autoconfigure.flyway.FlywayAutoConfiguration;
import org.springframework.context.annotation.Import;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.test.context.DynamicPropertyRegistry;
import org.springframework.test.context.DynamicPropertySource;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.context.bean.override.mockito.MockitoSpyBean;
import org.springframework.transaction.annotation.Propagation;
import org.springframework.transaction.annotation.Transactional;
import org.testcontainers.containers.PostgreSQLContainer;
import org.testcontainers.junit.jupiter.Container;
import org.testcontainers.junit.jupiter.Testcontainers;
import top.harrylei.bitlog.article.port.impl.ArticlePortImpl;
import top.harrylei.bitlog.article.repository.dao.ArticleDAO;
import top.harrylei.bitlog.article.repository.dao.ArticleStatisticsDAO;
import top.harrylei.bitlog.article.repository.dao.ArticleVersionDAO;
import top.harrylei.bitlog.comment.config.CommentConfiguration;
import top.harrylei.bitlog.comment.converter.CommentConverterImpl;
import top.harrylei.bitlog.comment.model.req.CommentSaveParam;
import top.harrylei.bitlog.comment.repository.dao.CommentDAO;
import top.harrylei.bitlog.comment.service.CommentService;
import top.harrylei.bitlog.comment.service.impl.CommentServiceImpl;
import top.harrylei.bitlog.common.config.MybatisPlusConfig;
import top.harrylei.bitlog.common.util.RateLimiter;
import top.harrylei.bitlog.notification.converter.NotificationConverterImpl;
import top.harrylei.bitlog.notification.listener.CommentNotificationListener;
import top.harrylei.bitlog.notification.model.enums.NotificationTypeEnum;
import top.harrylei.bitlog.notification.port.NotificationPort;
import top.harrylei.bitlog.notification.port.impl.NotificationPortImpl;
import top.harrylei.bitlog.notification.repository.dao.NotificationDAO;
import top.harrylei.bitlog.notification.repository.entity.NotificationDO;
import top.harrylei.bitlog.notification.service.impl.NotificationServiceImpl;
import top.harrylei.bitlog.user.port.UserPort;

/**
 * 发评论到通知派发的收件人推导集成测试
 * <p>
 * 覆盖点全部在「谁该收到通知」，而不是 dispatch 本身的去重与自我抑制（那部分由 NotificationDispatchIT 覆盖）。被回复者必须取 parent.getUserId()，直接回复根评论时
 * replyToUserId 被置空，只有这样才不会漏掉「直接回复楼主」的通知。
 * </p>
 * <p>
 * 用 {@code Propagation.NOT_SUPPORTED} 退出 {@code @MybatisPlusTest} 默认的测试事务包裹，
 * 否则监听器抛错触发的回滚只会在测试方法结束时才真正发生，测试方法内部读到的还是回滚前的数据。
 * </p>
 *
 * @author Harry
 * @since 2026-09-06
 */
@Testcontainers
@MybatisPlusTest
@Import({
    MybatisPlusConfig.class,
    ArticleDAO.class,
    ArticleVersionDAO.class,
    ArticleStatisticsDAO.class,
    ArticlePortImpl.class,
    CommentDAO.class,
    CommentConverterImpl.class,
    CommentConfiguration.class,
    CommentServiceImpl.class,
    NotificationDAO.class,
    NotificationConverterImpl.class,
    NotificationServiceImpl.class,
    NotificationPortImpl.class,
    CommentNotificationListener.class
})
@ImportAutoConfiguration(FlywayAutoConfiguration.class)
@Transactional(propagation = Propagation.NOT_SUPPORTED)
class CommentNotificationIT {

    private static final long OWNER = 960L;
    private static final long USER_A = 961L;
    private static final long USER_B = 962L;

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

    /** 限流走 Redis，与本测试的断言无关，一律放行 */
    @MockitoBean
    private RateLimiter rateLimiter;

    /** saveComment 不会用到，仅用于满足构造注入 */
    @MockitoBean
    private UserPort userPort;

    /** 默认透传真实实现，只有回滚测试临时改写为抛错 */
    @MockitoSpyBean
    private NotificationPort notificationPort;

    @Autowired
    private CommentService commentService;

    @Autowired
    private NotificationDAO notificationDAO;

    @Autowired
    private JdbcTemplate jdbc;

    private Long articleId1;
    private Long articleId2;

    @BeforeEach
    void seed() {
        when(rateLimiter.tryAcquire(any(), anyInt(), any())).thenReturn(RateLimiter.Result.allow());

        jdbc.update("DELETE FROM notification");
        jdbc.update("DELETE FROM comment");
        jdbc.update("DELETE FROM article_statistics");
        jdbc.update("UPDATE article SET published_version_id = NULL, latest_version_id = NULL");
        jdbc.update("DELETE FROM article_version");
        jdbc.update("DELETE FROM article");
        jdbc.update("DELETE FROM user_account WHERE id IN (?, ?, ?)", OWNER, USER_A, USER_B);

        insertUser(OWNER, "owner");
        insertUser(USER_A, "usera");
        insertUser(USER_B, "userb");

        articleId1 = insertPublishedArticle(OWNER, "OWNER 的文章");
        articleId2 = insertPublishedArticle(USER_A, "A 的文章");
    }

    private void insertUser(long id, String name) {
        jdbc.update(
                "INSERT INTO user_account (id, username, password, email, status, user_role, deleted) "
                        + "VALUES (?, ?, 'x', ?, 1, 0, 0)",
                id,
                name,
                name + "@example.com");
    }

    private Long insertPublishedArticle(long authorId, String title) {
        Long articleId = jdbc.queryForObject(
                "INSERT INTO article (user_id, summary, deleted) VALUES (?, '', 0) " + "RETURNING id",
                Long.class,
                authorId);
        Long versionId = jdbc.queryForObject(
                "INSERT INTO article_version (article_id, version, title, content) VALUES (?, 1, ?, 'content') "
                        + "RETURNING id",
                Long.class,
                articleId,
                title);
        jdbc.update(
                "UPDATE article SET published_version_id = ?, latest_version_id = ? WHERE id = ?",
                versionId,
                versionId,
                articleId);
        return articleId;
    }

    private CommentSaveParam param(Long parentId, String content) {
        CommentSaveParam param = new CommentSaveParam();
        param.setParentId(parentId);
        param.setContent(content);
        return param;
    }

    private List<NotificationDO> findByRecipientAndType(long recipientId, NotificationTypeEnum type) {
        return notificationDAO
                .lambdaQuery()
                .eq(NotificationDO::getRecipientId, recipientId)
                .eq(NotificationDO::getType, type)
                .list();
    }

    @Test
    @DisplayName("saveComment_回复根评论_楼主收到COMMENT_REPLY")
    void saveComment_replyToRootComment_ownerOfRootGetsCommentReply() {
        Long rootId = commentService.saveComment(USER_A, articleId1, param(null, "root by A"));
        Long replyId = commentService.saveComment(USER_B, articleId1, param(rootId, "reply by B"));

        List<NotificationDO> notifications = findByRecipientAndType(USER_A, NotificationTypeEnum.COMMENT_REPLY);
        assertThat(notifications).hasSize(1);
        assertThat(notifications.get(0).getTargetId()).isEqualTo(replyId);
        assertThat(notifications.get(0).getActorId()).isEqualTo(USER_B);
    }

    @Test
    @DisplayName("saveComment_回复子评论_子评论作者收到COMMENT_REPLY")
    void saveComment_replyToChildComment_childAuthorGetsCommentReply() {
        Long rootId = commentService.saveComment(OWNER, articleId1, param(null, "root by owner"));
        Long childByA = commentService.saveComment(USER_A, articleId1, param(rootId, "child by A"));
        Long replyId = commentService.saveComment(USER_B, articleId1, param(childByA, "reply to A's child"));

        List<NotificationDO> notifications = findByRecipientAndType(USER_A, NotificationTypeEnum.COMMENT_REPLY);
        assertThat(notifications).hasSize(1);
        assertThat(notifications.get(0).getTargetId()).isEqualTo(replyId);
        assertThat(notifications.get(0).getActorId()).isEqualTo(USER_B);
    }

    @Test
    @DisplayName("saveComment_自己回复自己_不产生COMMENT_REPLY")
    void saveComment_replyToOwnComment_noCommentReplyProduced() {
        Long rootId = commentService.saveComment(USER_A, articleId1, param(null, "root by A"));
        Long selfReplyId = commentService.saveComment(USER_A, articleId1, param(rootId, "A replies self"));

        boolean exists = notificationDAO
                .lambdaQuery()
                .eq(NotificationDO::getRecipientId, USER_A)
                .eq(NotificationDO::getType, NotificationTypeEnum.COMMENT_REPLY)
                .eq(NotificationDO::getTargetId, selfReplyId)
                .exists();
        assertThat(exists).isFalse();
    }

    @Test
    @DisplayName("saveComment_非作者评论文章_文章作者收到ARTICLE_COMMENT")
    void saveComment_nonAuthorComments_articleAuthorGetsArticleComment() {
        Long commentId = commentService.saveComment(USER_A, articleId1, param(null, "root by A"));

        List<NotificationDO> notifications = findByRecipientAndType(OWNER, NotificationTypeEnum.ARTICLE_COMMENT);
        assertThat(notifications).hasSize(1);
        assertThat(notifications.get(0).getTargetId()).isEqualTo(commentId);
        assertThat(notifications.get(0).getActorId()).isEqualTo(USER_A);
    }

    @Test
    @DisplayName("saveComment_作者评论自己的文章_不产生ARTICLE_COMMENT")
    void saveComment_authorCommentsOwnArticle_noArticleCommentProduced() {
        Long commentId = commentService.saveComment(USER_A, articleId2, param(null, "A comments own article"));

        boolean exists = notificationDAO
                .lambdaQuery()
                .eq(NotificationDO::getRecipientId, USER_A)
                .eq(NotificationDO::getType, NotificationTypeEnum.ARTICLE_COMMENT)
                .eq(NotificationDO::getTargetId, commentId)
                .exists();
        assertThat(exists).isFalse();
    }

    @Test
    @DisplayName("saveComment_监听器抛错_评论的落库随事务一并回滚")
    void saveComment_listenerFails_commentInsertRollsBackToo() {
        doThrow(new IllegalStateException("通知派发失败")).when(notificationPort).dispatch(anyList());
        try {
            assertThatThrownBy(() -> commentService.saveComment(USER_B, articleId1, param(null, "B comments")))
                    .isInstanceOf(IllegalStateException.class);

            Long count = jdbc.queryForObject(
                    "SELECT COUNT(*) FROM comment WHERE article_id = ? AND user_id = ?",
                    Long.class,
                    articleId1,
                    USER_B);
            assertThat(count).as("评论写入应随监听器异常一并回滚").isZero();
        } finally {
            Mockito.reset(notificationPort);
        }
    }
}
