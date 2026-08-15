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
import top.harrylei.bitlog.common.config.MybatisPlusConfig;
import top.harrylei.bitlog.link.repository.dao.FriendLinkDAO;
import top.harrylei.bitlog.link.repository.entity.FriendLinkDO;

import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * 友链审核与状态流转集成测试
 * <p>
 * 状态列在库里是 smallint，而 DAO 是把枚举直接交给 lambdaUpdate().set() 的。 项目其余地方的 set() 传的都是 Long/String，本类的首要目的就是钉住 「枚举经 Wrapper
 * 写入后落库的是数字码」这件事——写错了不会报错，只会静默写坏。
 * </p>
 *
 * @author Harry
 * @since 2026-08-15
 */
@Testcontainers
@MybatisPlusTest
@Import({MybatisPlusConfig.class, FriendLinkDAO.class})
@ImportAutoConfiguration(FlywayAutoConfiguration.class)
class FriendLinkAuditIT {

    private static final long APPLICANT = 910L;

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
        jdbc.update("""
            INSERT INTO user_account (id, username, email) VALUES (?, ?, ?)
            ON CONFLICT (id) DO NOTHING
            """, APPLICANT, "applicant-" + APPLICANT, "applicant-" + APPLICANT + "@test.local");
    }

    private Long insertPending(String name, String url) {
        FriendLinkDO link =
            new FriendLinkDO().setUserId(APPLICANT).setName(name).setUrl(url).setStatus(FriendLinkStatusEnum.PENDING);
        friendLinkDAO.save(link);
        return link.getId();
    }

    /** 站长录入的行 user_id 为 NULL，不受「一人一条」约束，适合造多行 */
    private Long insertOwnerless(String name, String url, FriendLinkStatusEnum status) {
        FriendLinkDO link = new FriendLinkDO().setName(name).setUrl(url).setStatus(status);
        friendLinkDAO.save(link);
        return link.getId();
    }

    private Integer rawStatus(Long id) {
        return jdbc.queryForObject("SELECT status FROM friend_link WHERE id = ?", Integer.class, id);
    }

    @Test
    @DisplayName("save_持久化枚举_落库为数字码")
    void save_enumStatus_storedAsCode() {
        Long id = insertPending("站点", "https://a.example.com");

        assertThat(rawStatus(id)).isEqualTo(FriendLinkStatusEnum.PENDING.getCode()).isZero();
        assertThat(friendLinkDAO.getById(id).getStatus()).isEqualTo(FriendLinkStatusEnum.PENDING);
    }

    @Test
    @DisplayName("updateStatus_经Wrapper写枚举_落库为数字码而非枚举名")
    void updateStatus_enumViaWrapper_storedAsCode() {
        Long id = insertPending("站点", "https://a.example.com");

        friendLinkDAO.updateStatus(id, FriendLinkStatusEnum.APPROVED, null);

        // 这是本类存在的理由：写成枚举名的话这里会拿到非法值或抛类型异常
        assertThat(rawStatus(id)).isEqualTo(FriendLinkStatusEnum.APPROVED.getCode()).isEqualTo(1);
        assertThat(friendLinkDAO.getById(id).getStatus()).isEqualTo(FriendLinkStatusEnum.APPROVED);
    }

    @Test
    @DisplayName("updateStatus_通过时传null_拒绝理由被真正清空")
    void updateStatus_approveWithNullReason_clearsRejectReason() {
        Long id = insertPending("站点", "https://a.example.com");
        friendLinkDAO.updateStatus(id, FriendLinkStatusEnum.REJECTED, "没找到回链");
        assertThat(friendLinkDAO.getById(id).getRejectReason()).isEqualTo("没找到回链");

        friendLinkDAO.updateStatus(id, FriendLinkStatusEnum.APPROVED, null);

        // MyBatis-Plus 默认 NOT_NULL 更新策略会把 null 从 SET 子句剔掉，
        // DAO 用显式 set 正是为了绕开它；退回 updateById 的话这里会留着旧文案
        assertThat(friendLinkDAO.getById(id).getRejectReason()).isNull();
    }

    @Test
    @DisplayName("updateContent_清空选填字段_真的写入null")
    void updateContent_blankOptionalFields_persistedAsNull() {
        Long id = insertPending("站点", "https://a.example.com");
        FriendLinkDO seeded = friendLinkDAO.getById(id);
        seeded.setAvatar("https://a.example.com/a.png").setDescription("简介").setApplyMessage("留言");
        friendLinkDAO.updateContent(seeded);
        assertThat(friendLinkDAO.getById(id).getAvatar()).isNotNull();

        FriendLinkDO cleared = friendLinkDAO.getById(id);
        cleared.setAvatar(null).setDescription(null).setApplyMessage(null);
        friendLinkDAO.updateContent(cleared);

        FriendLinkDO after = friendLinkDAO.getById(id);
        assertThat(after.getAvatar()).isNull();
        assertThat(after.getDescription()).isNull();
        assertThat(after.getApplyMessage()).isNull();
    }

    @Test
    @DisplayName("updateContent_不触碰状态列_与并发审核不互相覆盖")
    void updateContent_leavesStatusUntouched() {
        Long id = insertPending("站点", "https://a.example.com");
        friendLinkDAO.updateStatus(id, FriendLinkStatusEnum.APPROVED, null);

        // 拿的是审核之前的快照，若 updateContent 把 status 一并写回就会退回 PENDING
        FriendLinkDO stale = new FriendLinkDO().setName("改名").setUrl("https://a.example.com");
        stale.setId(id);
        friendLinkDAO.updateContent(stale);

        FriendLinkDO after = friendLinkDAO.getById(id);
        assertThat(after.getName()).isEqualTo("改名");
        assertThat(after.getStatus()).isEqualTo(FriendLinkStatusEnum.APPROVED);
    }

    @Test
    @DisplayName("listApproved_只取已通过且早加入的在前")
    void listApproved_ordersOldestFirst() {
        Long first = insertOwnerless("最早", "https://1.example.com", FriendLinkStatusEnum.APPROVED);
        Long second = insertOwnerless("其次", "https://2.example.com", FriendLinkStatusEnum.APPROVED);
        Long pending = insertOwnerless("待审", "https://3.example.com", FriendLinkStatusEnum.PENDING);

        List<FriendLinkDO> approved = friendLinkDAO.listApproved();

        assertThat(approved).extracting(FriendLinkDO::getId).containsExactly(first, second).doesNotContain(pending);
    }

    @Test
    @DisplayName("detachOwner_注销后解除归属_友链本身保留")
    void detachOwner_keepsLinkDropsOwner() {
        Long id = insertPending("站点", "https://a.example.com");

        friendLinkDAO.detachOwner(APPLICANT);

        FriendLinkDO after = friendLinkDAO.getById(id);
        assertThat(after).isNotNull();
        assertThat(after.getUserId()).isNull();
        assertThat(friendLinkDAO.getByUserId(APPLICANT)).isNull();
    }

    @Test
    @DisplayName("uk_friend_link_user_多条无归属的行可共存")
    void ownerlessLinks_coexist() {
        insertOwnerless("站长录入A", "https://x.example.com", FriendLinkStatusEnum.APPROVED);
        insertOwnerless("站长录入B", "https://y.example.com", FriendLinkStatusEnum.APPROVED);

        // UNIQUE(user_id) 视 NULL 互不相等，站长录入的行才不受「一人一条」约束
        assertThat(friendLinkDAO.listApproved()).hasSize(2);
    }
}
