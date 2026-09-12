package top.harrylei.bitlog.server;

import static org.assertj.core.api.Assertions.*;
import static org.mockito.Mockito.*;

import com.baomidou.mybatisplus.test.autoconfigure.MybatisPlusTest;
import java.util.List;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.Executors;
import java.util.concurrent.TimeUnit;
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
import org.springframework.transaction.annotation.Propagation;
import org.springframework.transaction.annotation.Transactional;
import org.testcontainers.containers.PostgreSQLContainer;
import org.testcontainers.junit.jupiter.Container;
import org.testcontainers.junit.jupiter.Testcontainers;
import top.harrylei.bitlog.article.component.ArticleReadDedupe;
import top.harrylei.bitlog.article.component.DeployHookService;
import top.harrylei.bitlog.article.converter.ArticleConverterImpl;
import top.harrylei.bitlog.article.model.req.*;
import top.harrylei.bitlog.article.model.vo.ArticleSaveVO;
import top.harrylei.bitlog.article.repository.dao.*;
import top.harrylei.bitlog.article.service.ArticleService;
import top.harrylei.bitlog.article.service.CategoryService;
import top.harrylei.bitlog.article.service.TagService;
import top.harrylei.bitlog.article.service.impl.*;
import top.harrylei.bitlog.common.config.MybatisPlusConfig;
import top.harrylei.bitlog.common.enums.ResultCode;
import top.harrylei.bitlog.common.exception.BusinessException;

/**
 * 文章编辑版本冲突与分类标签外键回归测试。
 *
 * @author Harry
 * @since 2026-09-11
 */
@Testcontainers
@MybatisPlusTest
@Import({
    MybatisPlusConfig.class,
    ArticleDAO.class,
    ArticleVersionDAO.class,
    ArticleTagDAO.class,
    ArticleStatisticsDAO.class,
    CategoryDAO.class,
    TagDAO.class,
    ArticleConverterImpl.class,
    ArticleServiceImpl.class,
    CategoryServiceImpl.class,
    TagServiceImpl.class
})
@ImportAutoConfiguration(FlywayAutoConfiguration.class)
class ArticleEditingIT {
    private static final long AUTHOR = 990L;

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
        registry.add("mybatis-plus.mapper-locations", () -> "classpath*:mapper/*.xml");
    }

    @MockitoBean
    private DeployHookService deployHookService;

    @MockitoBean
    private ArticleReadDedupe articleReadDedupe;

    @Autowired
    private ArticleService articleService;

    @Autowired
    private CategoryService categoryService;

    @Autowired
    private TagService tagService;

    @Autowired
    private JdbcTemplate jdbc;

    @BeforeEach
    void seed() {
        jdbc.update(
                "INSERT INTO user_account (id, username, email) VALUES (?, 'audit-author', 'audit@example.com') ON CONFLICT (id) DO NOTHING",
                AUTHOR);
    }

    private ArticleSaveVO draft() {
        return articleService.saveArticle(
                AUTHOR, new ArticleSaveParam().setTitle("初始标题").setContent("初始正文"));
    }

    private ArticleUpdateParam update(Long expected) {
        ArticleUpdateParam param = new ArticleUpdateParam().setExpectedVersionId(expected);
        param.setTitle("更新标题").setContent("更新正文");
        return param;
    }

    private Long category() {
        return jdbc.queryForObject("INSERT INTO category (name) VALUES ('编辑分类') RETURNING id", Long.class);
    }

    private void assertConflict(Runnable action) {
        assertThatThrownBy(action::run)
                .isInstanceOfSatisfying(
                        BusinessException.class,
                        e -> assertThat(e.getCode()).isEqualTo(ResultCode.ARTICLE_VERSION_CONFLICT.getCode()));
    }

    @Test
    void publishArticle_staleConfirmedVersion_doesNotPublishOtherEditorsContent() {
        ArticleSaveVO first = draft();
        ArticleSaveVO second = articleService.updateArticle(AUTHOR, first.getId(), update(first.getVersionId()));
        Long category = category();

        assertConflict(() -> articleService.publishArticle(
                AUTHOR,
                first.getId(),
                new ArticlePublishParam()
                        .setExpectedVersionId(first.getVersionId())
                        .setCategoryId(category)));

        assertThat(jdbc.queryForObject(
                        "SELECT published_version_id FROM article WHERE id = ?", Long.class, first.getId()))
                .isNull();
        assertThat(second.getVersionId()).isNotEqualTo(first.getVersionId());
        verifyNoInteractions(deployHookService);
    }

    @Test
    void publishArticle_currentConfirmedVersion_publishesExactSavedContent() {
        ArticleSaveVO saved = draft();
        articleService.publishArticle(
                AUTHOR,
                saved.getId(),
                new ArticlePublishParam()
                        .setExpectedVersionId(saved.getVersionId())
                        .setCategoryId(category()));

        assertThat(jdbc.queryForObject(
                        "SELECT published_version_id FROM article WHERE id = ?", Long.class, saved.getId()))
                .isEqualTo(saved.getVersionId());
        verify(deployHookService).triggerDeploy();
    }

    @Test
    void updateArticle_staleBase_doesNotAppendOrMoveDraftHead() {
        ArticleSaveVO first = draft();
        ArticleSaveVO second = articleService.updateArticle(AUTHOR, first.getId(), update(first.getVersionId()));

        assertConflict(() -> articleService.updateArticle(AUTHOR, first.getId(), update(first.getVersionId())));

        assertThat(jdbc.queryForObject("SELECT latest_version_id FROM article WHERE id = ?", Long.class, first.getId()))
                .isEqualTo(second.getVersionId());
        assertThat(jdbc.queryForObject(
                        "SELECT COUNT(*) FROM article_version WHERE article_id = ?", Integer.class, first.getId()))
                .isEqualTo(2);
    }

    @Test
    @Transactional(propagation = Propagation.NOT_SUPPORTED)
    void updateArticle_twoRequestsWithSameBase_onlyOneAppendsVersion() throws Exception {
        ArticleSaveVO first = draft();
        CountDownLatch start = new CountDownLatch(1);
        try (var executor = Executors.newFixedThreadPool(2)) {
            java.util.concurrent.Callable<Integer> save = () -> {
                if (!start.await(10, TimeUnit.SECONDS)) throw new IllegalStateException("start timeout");
                try {
                    articleService.updateArticle(AUTHOR, first.getId(), update(first.getVersionId()));
                    return 0;
                } catch (BusinessException e) {
                    return e.getCode();
                }
            };
            var a = executor.submit(save);
            var b = executor.submit(save);
            start.countDown();
            assertThat(List.of(a.get(15, TimeUnit.SECONDS), b.get(15, TimeUnit.SECONDS)))
                    .containsExactlyInAnyOrder(0, ResultCode.ARTICLE_VERSION_CONFLICT.getCode());
        }
        assertThat(jdbc.queryForObject("SELECT version_count FROM article WHERE id = ?", Integer.class, first.getId()))
                .isEqualTo(2);
        assertThat(jdbc.queryForObject(
                        "SELECT COUNT(*) FROM article_version WHERE article_id = ?", Integer.class, first.getId()))
                .isEqualTo(2);
    }

    @Test
    void batchDelete_referencedTag_removesLinksBeforeTag() {
        ArticleSaveVO saved = draft();
        Long tag = jdbc.queryForObject("INSERT INTO tag (name) VALUES ('标签') RETURNING id", Long.class);
        jdbc.update("INSERT INTO article_tag (article_id, tag_id) VALUES (?, ?)", saved.getId(), tag);

        tagService.batchDelete(List.of(tag));

        assertThat(jdbc.queryForObject("SELECT COUNT(*) FROM tag WHERE id = ?", Integer.class, tag))
                .isZero();
        assertThat(jdbc.queryForObject("SELECT COUNT(*) FROM article_tag WHERE tag_id = ?", Integer.class, tag))
                .isZero();
    }

    @Test
    void delete_draftAndDeletedArticleReferences_clearsCategoryWithoutDeletingArticles() {
        Long category = category();
        ArticleSaveVO draft = draft();
        ArticleSaveVO deleted = draft();
        jdbc.update("UPDATE article SET category_id = ? WHERE id IN (?, ?)", category, draft.getId(), deleted.getId());
        jdbc.update(
                "UPDATE article SET deleted = 1, published_version_id = latest_version_id WHERE id = ?",
                deleted.getId());

        categoryService.delete(category);

        assertThat(jdbc.queryForObject("SELECT COUNT(*) FROM category WHERE id = ?", Integer.class, category))
                .isZero();
        assertThat(jdbc.queryForList(
                        "SELECT category_id FROM article WHERE id IN (?, ?)",
                        Long.class,
                        draft.getId(),
                        deleted.getId()))
                .containsOnlyNulls();
        assertThat(jdbc.queryForObject(
                        "SELECT COUNT(*) FROM article_version WHERE article_id IN (?, ?)",
                        Integer.class,
                        draft.getId(),
                        deleted.getId()))
                .isEqualTo(2);
    }

    @Test
    void delete_publishedArticleReference_rejectsAndPreservesCategory() {
        Long category = category();
        ArticleSaveVO saved = draft();
        jdbc.update(
                "UPDATE article SET category_id = ?, published_version_id = latest_version_id WHERE id = ?",
                category,
                saved.getId());

        assertThatThrownBy(() -> categoryService.delete(category))
                .isInstanceOfSatisfying(
                        BusinessException.class,
                        e -> assertThat(e.getCode()).isEqualTo(ResultCode.CATEGORY_HAS_ARTICLES.getCode()));

        assertThat(jdbc.queryForObject("SELECT category_id FROM article WHERE id = ?", Long.class, saved.getId()))
                .isEqualTo(category);
    }
}
