package top.harrylei.bitlog.server;

import com.baomidou.mybatisplus.core.metadata.IPage;
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
import top.harrylei.bitlog.article.model.enums.MyArticleSortEnum;
import top.harrylei.bitlog.article.model.query.MyArticlePageParam;
import top.harrylei.bitlog.article.repository.dao.ArticleDAO;
import top.harrylei.bitlog.article.repository.entity.ArticleDO;
import top.harrylei.bitlog.common.config.MybatisPlusConfig;
import top.harrylei.bitlog.common.enums.SortOrderEnum;

import java.time.LocalDate;
import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * 文章分页排序集成测试，验证派生列与稳定次序键在真实 PostgreSQL 上的行为
 *
 * @author Harry
 * @since 2026-08-07
 */
@Testcontainers
@MybatisPlusTest
@Import({MybatisPlusConfig.class, ArticleDAO.class})
@ImportAutoConfiguration(FlywayAutoConfiguration.class)
class ArticlePaginationIT {

    private static final long AUTHOR = 900L;

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
    private ArticleDAO articleDAO;

    @Autowired
    private JdbcTemplate jdbc;

    @BeforeEach
    void seed() {
        jdbc.update("DELETE FROM article_version WHERE article_id IN (SELECT id FROM article WHERE user_id = ?)",
            AUTHOR);
        jdbc.update("DELETE FROM article WHERE user_id = ?", AUTHOR);

        jdbc.update("""
            INSERT INTO user_account (id, username, email) VALUES (?, ?, ?)
            ON CONFLICT (id) DO NOTHING
            """, AUTHOR, "author-" + AUTHOR, "author-" + AUTHOR + "@test.local");

        // A 已发布：创建最早、发布居中 B 已发布：创建最晚、发布最早
        // C 纯草稿：从未发布 D 已下架：发布时间最晚但已取消发布
        insert("A", "2024-01-01", "2024-06-01", true);
        insert("B", "2024-05-01", "2024-02-01", true);
        insert("C", "2024-03-01", null, false);
        insert("D", "2024-04-01", "2024-12-01", false);
    }

    private void insert(String tag, String createTime, String publishTime, boolean published) {
        jdbc.update("""
            INSERT INTO article (user_id, summary, deleted, create_time, publish_time, version_count)
            VALUES (?, ?, 0, ?, ?, 1)
            """, AUTHOR, tag, LocalDate.parse(createTime).atStartOfDay(),
            publishTime == null ? null : LocalDate.parse(publishTime).atStartOfDay());
        Long articleId =
            jdbc.queryForObject("SELECT id FROM article WHERE user_id = ? AND summary = ?", Long.class, AUTHOR, tag);
        jdbc.update("INSERT INTO article_version (article_id, version, title, content) VALUES (?, 1, ?, '正文')",
            articleId, "标题-" + tag);
        Long versionId =
            jdbc.queryForObject("SELECT id FROM article_version WHERE article_id = ?", Long.class, articleId);
        jdbc.update("UPDATE article SET latest_version_id = ?, published_version_id = ? WHERE id = ?", versionId,
            published ? versionId : null, articleId);
    }

    private List<String> order(MyArticlePageParam param) {
        IPage<ArticleDO> page = articleDAO.pageByUser(AUTHOR, param, param.toPage());
        return page.getRecords().stream().map(ArticleDO::getSummary).toList();
    }

    /**
     * publish_time 是首次发布时间、下架后不清空。若排序退化成 COALESCE(publish_time, create_time)， 下架的 D 会带着 2024-12 窜到首位，而界面显示的是它的创建时间
     * 2024-04
     */
    @Test
    @DisplayName("展示时间排序：下架文章按创建时间参与排序，不被旧发布时间顶起")
    void pageByUser_displayTimeDesc_unpublishedArticleFallsBackToCreateTime() {
        MyArticlePageParam param = new MyArticlePageParam().setSortField(MyArticleSortEnum.DISPLAY_TIME);

        assertThat(order(param)).containsExactly("A", "D", "C", "B");
    }

    @Test
    @DisplayName("展示时间升序")
    void pageByUser_displayTimeAsc_reversesOrder() {
        MyArticlePageParam param =
            new MyArticlePageParam().setSortField(MyArticleSortEnum.DISPLAY_TIME).setSortOrder(SortOrderEnum.ASC);

        assertThat(order(param)).containsExactly("B", "C", "D", "A");
    }

    @Test
    @DisplayName("默认按创建时间倒序，与展示时间排序结果不同，二者不可互相顶替")
    void pageByUser_defaultSort_ordersByCreateTime() {
        assertThat(order(new MyArticlePageParam())).containsExactly("B", "D", "C", "A");
    }

    @Test
    @DisplayName("创建时间相同的行由稳定次序键决定顺序，翻页不重复不丢行")
    void pageByUser_equalSortValues_areOrderedDeterministically() {
        jdbc.update("UPDATE article SET create_time = '2024-01-01 00:00:00' WHERE user_id = ?", AUTHOR);

        MyArticlePageParam first = new MyArticlePageParam();
        first.setPageSize(2);
        MyArticlePageParam second = new MyArticlePageParam();
        second.setPageSize(2);
        second.setPageNum(2);

        List<String> pageOne = order(first);
        List<String> pageTwo = order(second);

        assertThat(pageOne).hasSize(2);
        assertThat(pageTwo).hasSize(2);
        assertThat(pageOne).doesNotContainAnyElementsOf(pageTwo);
        assertThat(order(first)).as("同值行的顺序必须可重复").isEqualTo(pageOne);
    }

    @Test
    @DisplayName("状态过滤经布尔翻译落到已发布/未发布两条件上")
    void pageByUser_statusFilter_splitsPublishedAndDraft() {
        MyArticlePageParam published =
            new MyArticlePageParam().setStatus(top.harrylei.bitlog.article.model.enums.ArticleStatusEnum.PUBLISHED);
        MyArticlePageParam draft =
            new MyArticlePageParam().setStatus(top.harrylei.bitlog.article.model.enums.ArticleStatusEnum.DRAFT);

        assertThat(order(published)).containsExactlyInAnyOrder("A", "B");
        assertThat(order(draft)).containsExactlyInAnyOrder("C", "D");
    }

    @Test
    @DisplayName("超过上限的每页大小被分页插件截断，不会全表返回")
    void pageByUser_oversizedPageSize_isCappedByInterceptor() {
        MyArticlePageParam param = new MyArticlePageParam();
        param.setPageSize(100000);

        IPage<ArticleDO> page = articleDAO.pageByUser(AUTHOR, param, param.toPage());

        assertThat(page.getRecords()).hasSizeLessThanOrEqualTo(top.harrylei.bitlog.common.model.BasePage.MAX_PAGE_SIZE);
    }
}
