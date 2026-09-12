package top.harrylei.bitlog.server;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.when;

import com.baomidou.mybatisplus.test.autoconfigure.MybatisPlusTest;
import org.junit.jupiter.api.BeforeEach;
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
import top.harrylei.bitlog.article.port.ArticlePort;
import top.harrylei.bitlog.comment.config.CommentProperties;
import top.harrylei.bitlog.comment.converter.CommentConverterImpl;
import top.harrylei.bitlog.comment.model.query.CommentPageParam;
import top.harrylei.bitlog.comment.model.vo.CommentVO;
import top.harrylei.bitlog.comment.repository.dao.CommentDAO;
import top.harrylei.bitlog.comment.service.CommentService;
import top.harrylei.bitlog.comment.service.impl.CommentServiceImpl;
import top.harrylei.bitlog.common.config.MybatisPlusConfig;
import top.harrylei.bitlog.common.util.RateLimiter;
import top.harrylei.bitlog.user.port.UserPort;

/**
 * 评论可见性在分页前生效，列表与计数遵循相同的墓碑规则。
 *
 * @author Harry
 * @since 2026-09-11
 */
@Testcontainers
@MybatisPlusTest
@Import({MybatisPlusConfig.class, CommentDAO.class, CommentConverterImpl.class, CommentServiceImpl.class})
@ImportAutoConfiguration(FlywayAutoConfiguration.class)
class CommentPaginationIT {
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
        registry.add(
                "mybatis-plus.configuration.default-enum-type-handler",
                () -> "com.baomidou.mybatisplus.core.handlers.MybatisEnumTypeHandler");
    }

    @MockitoBean
    private ArticlePort articlePort;

    @MockitoBean
    private UserPort userPort;

    @MockitoBean
    private RateLimiter rateLimiter;

    @MockitoBean
    private CommentProperties commentProperties;

    @Autowired
    private CommentService commentService;

    @Autowired
    private JdbcTemplate jdbc;

    private Long articleId;

    @BeforeEach
    void seed() {
        jdbc.update(
                "INSERT INTO user_account (id, username, email) VALUES (991, 'comment-author', 'comment@example.com')");
        articleId = jdbc.queryForObject("INSERT INTO article (user_id) VALUES (991) RETURNING id", Long.class);
        when(articlePort.isPublished(articleId)).thenReturn(true);
    }

    private Long comment(Long rootId, int status, int deleted) {
        return jdbc.queryForObject(
                "INSERT INTO comment (article_id, user_id, root_id, content, status, deleted) VALUES (?, 991, ?, '正文', ?, ?) RETURNING id",
                Long.class,
                articleId,
                rootId,
                status,
                deleted);
    }

    @Test
    void pageComments_invisibleRootsBeforeVisibleOnes_doesNotReturnEmptyFirstPage() {
        Long visible = comment(null, 1, 0);
        for (int i = 0; i < 12; i++) comment(null, 2, 0);
        CommentPageParam query = new CommentPageParam();
        query.setPageSize(10);

        var page = commentService.pageComments(articleId, query);

        assertThat(page.getContent()).extracting(CommentVO::getId).containsExactly(visible);
        assertThat(page.getTotalElements()).isEqualTo(1);
        assertThat(page.isHasNext()).isFalse();
    }

    @Test
    void pageComments_mixedVisibility_countsOnlyVisibleRootsAndRequiredTombstones() {
        Long visible = comment(null, 1, 0);
        comment(null, 2, 0);
        comment(null, 1, 1);
        Long hiddenWithReply = comment(null, 2, 0);
        comment(hiddenWithReply, 1, 0);
        Long deletedWithHiddenReply = comment(null, 1, 1);
        comment(deletedWithHiddenReply, 2, 0);
        Long deletedWithVisibleReply = comment(null, 1, 1);
        comment(deletedWithVisibleReply, 1, 0);
        comment(deletedWithVisibleReply, 1, 1);

        CommentPageParam query = new CommentPageParam();
        query.setPageSize(2);
        var first = commentService.pageComments(articleId, query);
        query.setPageNum(2);
        var second = commentService.pageComments(articleId, query);

        assertThat(first.getTotalElements()).isEqualTo(3);
        assertThat(first.getContent())
                .extracting(CommentVO::getId)
                .containsExactly(deletedWithVisibleReply, hiddenWithReply);
        assertThat(first.getContent()).allSatisfy(root -> {
            assertThat(root.getRemoved()).isTrue();
            assertThat(root.getContent()).isNull();
            assertThat(root.getReplies()).hasSize(1);
        });
        assertThat(first.isHasNext()).isTrue();
        assertThat(second.getContent()).extracting(CommentVO::getId).containsExactly(visible);
        assertThat(second.getTotalElements()).isEqualTo(3);
        assertThat(second.isHasNext()).isFalse();
    }
}
