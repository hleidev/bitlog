package top.harrylei.bitlog.article.model.query;

import static org.assertj.core.api.Assertions.assertThat;

import com.baomidou.mybatisplus.core.metadata.OrderItem;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.EnumSource;
import top.harrylei.bitlog.article.model.enums.ArticleStatusEnum;
import top.harrylei.bitlog.article.model.enums.MyArticleSortEnum;
import top.harrylei.bitlog.common.enums.SortOrderEnum;

/**
 * 我的文章查询参数测试
 *
 * @author Harry
 * @since 2026-08-07
 */
class MyArticlePageParamTest {

    /**
     * XML 里做 OGNL 字符串比较会踩到单字符字面量按 char 解析的坑，故状态过滤翻译成布尔值
     */
    @Test
    @DisplayName("不传 status 时不过滤发布状态")
    void getPublishedOnly_statusNotProvided_returnsNull() {
        assertThat(new MyArticlePageParam().getPublishedOnly()).isNull();
    }

    @Test
    @DisplayName("已发布映射为 true")
    void getPublishedOnly_statusPublished_returnsTrue() {
        assertThat(new MyArticlePageParam()
                        .setStatus(ArticleStatusEnum.PUBLISHED)
                        .getPublishedOnly())
                .isTrue();
    }

    @Test
    @DisplayName("草稿映射为 false")
    void getPublishedOnly_statusDraft_returnsFalse() {
        assertThat(new MyArticlePageParam().setStatus(ArticleStatusEnum.DRAFT).getPublishedOnly())
                .isFalse();
    }

    @ParameterizedTest
    @EnumSource(ArticleStatusEnum.class)
    @DisplayName("每个状态取值都有确定的布尔映射，新增枚举值必须同步翻译逻辑")
    void getPublishedOnly_everyStatus_isExhaustivelyMapped(ArticleStatusEnum status) {
        assertThat(new MyArticlePageParam().setStatus(status).getPublishedOnly())
                .as("%s 未映射", status)
                .isNotNull();
    }

    @Test
    @DisplayName("不传排序时按创建时间倒序")
    void resolveSortField_notProvided_defaultsToCreateTime() {
        assertThat(new MyArticlePageParam().toPage().orders())
                .extracting(OrderItem::getColumn, OrderItem::isAsc)
                .containsExactly(
                        org.assertj.core.api.Assertions.tuple(MyArticleSortEnum.CREATE_TIME.getColumn(), false),
                        org.assertj.core.api.Assertions.tuple("a.id", false));
    }

    @Test
    @DisplayName("更新时间倒序并以文章 ID 倒序稳定分页")
    void resolveSortField_updateTime_ordersByUpdateTimeThenId() {
        MyArticlePageParam param = new MyArticlePageParam().setSortField(MyArticleSortEnum.UPDATE_TIME);

        assertThat(param.toPage().orders())
                .extracting(OrderItem::getColumn, OrderItem::isAsc)
                .containsExactly(
                        org.assertj.core.api.Assertions.tuple("a.update_time", false),
                        org.assertj.core.api.Assertions.tuple("a.id", false));
    }

    @Test
    @DisplayName("展示时间排序取 SELECT 中的派生列别名")
    void resolveSortField_displayTime_usesDerivedColumnAlias() {
        MyArticlePageParam param = new MyArticlePageParam().setSortField(MyArticleSortEnum.DISPLAY_TIME);

        assertThat(param.toPage().orders().getFirst().getColumn()).isEqualTo("display_time");
    }

    @Test
    @DisplayName("显式指定升序时覆盖排序键默认方向")
    void resolveSortOrder_ascProvided_overridesDefault() {
        MyArticlePageParam param = new MyArticlePageParam()
                .setSortField(MyArticleSortEnum.DISPLAY_TIME)
                .setSortOrder(SortOrderEnum.ASC);

        assertThat(param.toPage().orders().getFirst().isAsc()).isTrue();
    }

    @Test
    @DisplayName("公开列表与管理列表各用各的排序枚举，公开端不暴露草稿相关排序")
    void sortEnums_publicAndAdmin_areSeparate() {
        assertThat(MyArticleSortEnum.values()).extracting(Enum::name).contains("CREATE_TIME", "DISPLAY_TIME");
        assertThat(top.harrylei.bitlog.article.model.enums.ArticleSortEnum.values())
                .extracting(Enum::name)
                .containsExactly("PUBLISH_TIME");
    }
}
