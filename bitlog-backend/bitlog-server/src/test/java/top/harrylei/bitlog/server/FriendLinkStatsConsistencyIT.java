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
import top.harrylei.bitlog.api.enums.link.FriendLinkStatusEnum;
import top.harrylei.bitlog.api.model.link.dto.FriendLinkStatsDTO;
import top.harrylei.bitlog.api.model.link.query.FriendLinkPageParam;
import top.harrylei.bitlog.common.config.MybatisPlusConfig;
import top.harrylei.bitlog.link.repository.dao.FriendLinkDAO;
import top.harrylei.bitlog.link.repository.entity.FriendLinkDO;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * 友链 tab 计数与列表口径一致性测试
 * <p>
 * 计数和分页是两个入口，但必须落在同一批行上：tab 上写着 3，列表就该有 3 行。 两者共用 {@code FriendLinkDAO#adminBaseQuery}，本类钉住的正是这个共用关系——
 * 一旦有人只改了其中一侧的关键词谓词，计数就会和列表对不上，而这种偏差在界面上 表现得很轻微（数字略有出入），没人会立刻发现。
 * </p>
 *
 * @author Harry
 * @since 2026-08-16
 */
@Testcontainers
@MybatisPlusTest
@Import({MybatisPlusConfig.class, FriendLinkDAO.class})
@ImportAutoConfiguration(FlywayAutoConfiguration.class)
class FriendLinkStatsConsistencyIT {

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
    private FriendLinkDAO friendLinkDAO;

    @Autowired
    private JdbcTemplate jdbc;

    @BeforeEach
    void seed() {
        jdbc.update("DELETE FROM friend_link");
        // user_id 留空：站长手动添加的行不受「一人一条」唯一约束限制，便于造多行
        save("Alpha 博客", "https://alpha.example.com", FriendLinkStatusEnum.APPROVED);
        save("Beta 手记", "https://beta.example.com", FriendLinkStatusEnum.APPROVED);
        save("Alpha 实验室", "https://lab.example.com", FriendLinkStatusEnum.PENDING);
        save("Gamma 站", "https://gamma.example.com", FriendLinkStatusEnum.REJECTED);
    }

    private void save(String name, String url, FriendLinkStatusEnum status) {
        friendLinkDAO.save(new FriendLinkDO().setName(name).setUrl(url).setStatus(status));
    }

    private FriendLinkPageParam params(String keyword, FriendLinkStatusEnum status) {
        FriendLinkPageParam param = new FriendLinkPageParam();
        param.setKeyword(keyword);
        param.setStatus(status);
        return param;
    }

    /** 计数的某一档 == 同样条件下分页返回的总行数 */
    private void assertBucketMatchesPage(String keyword, FriendLinkStatusEnum status, long bucketCount) {
        FriendLinkPageParam param = params(keyword, status);
        long pageTotal = friendLinkDAO.pageForAdmin(param, param.toPage()).getTotal();
        assertThat(bucketCount).as("keyword=%s status=%s 的计数应与列表总数一致", keyword, status).isEqualTo(pageTotal);
    }

    @Test
    @DisplayName("countStats_noKeyword_matchesPageTotals")
    void countStats_noKeyword_matchesPageTotals() {
        FriendLinkStatsDTO stats = friendLinkDAO.countStats(params(null, null));

        assertThat(stats.getTotal()).isEqualTo(4);
        assertBucketMatchesPage(null, null, stats.getTotal());
        assertBucketMatchesPage(null, FriendLinkStatusEnum.PENDING, stats.getPending());
        assertBucketMatchesPage(null, FriendLinkStatusEnum.APPROVED, stats.getApproved());
        assertBucketMatchesPage(null, FriendLinkStatusEnum.REJECTED, stats.getRejected());
    }

    @Test
    @DisplayName("countStats_withKeyword_matchesPageTotals")
    void countStats_withKeyword_matchesPageTotals() {
        // 「Alpha」命中两行且跨越两个状态档，能同时验证关键词与分桶的组合
        FriendLinkStatsDTO stats = friendLinkDAO.countStats(params("Alpha", null));

        assertThat(stats.getTotal()).isEqualTo(2);
        assertThat(stats.getApproved()).isEqualTo(1);
        assertThat(stats.getPending()).isEqualTo(1);
        assertThat(stats.getRejected()).isZero();

        assertBucketMatchesPage("Alpha", null, stats.getTotal());
        assertBucketMatchesPage("Alpha", FriendLinkStatusEnum.PENDING, stats.getPending());
        assertBucketMatchesPage("Alpha", FriendLinkStatusEnum.APPROVED, stats.getApproved());
        assertBucketMatchesPage("Alpha", FriendLinkStatusEnum.REJECTED, stats.getRejected());
    }

    @Test
    @DisplayName("countStats_keywordMatchesUrl_notOnlyName")
    void countStats_keywordMatchesUrl_notOnlyName() {
        // 列表的关键词同时命中名称与地址，计数必须保持同样口径
        FriendLinkStatsDTO stats = friendLinkDAO.countStats(params("lab.example", null));

        assertThat(stats.getTotal()).isEqualTo(1);
        assertBucketMatchesPage("lab.example", null, stats.getTotal());
    }
}
