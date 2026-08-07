package top.harrylei.bitlog.common.model;

import com.baomidou.mybatisplus.core.metadata.OrderItem;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.Setter;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import top.harrylei.bitlog.common.enums.SortOrderEnum;

import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.tuple;

/**
 * 分页基础类测试
 *
 * @author Harry
 * @since 2026-08-07
 */
class BasePageTest {

    @Getter
    @AllArgsConstructor
    private enum TestSortField implements SortField {

        CREATE_TIME("create_time", SortOrderEnum.DESC), TITLE("title", SortOrderEnum.ASC), ID("id", SortOrderEnum.DESC);

        private final String column;
        private final SortOrderEnum defaultOrder;
    }

    @Setter
    private static class TestPageParam extends BasePage {

        private SortField sortField = TestSortField.CREATE_TIME;
        private SortOrderEnum sortOrder;
        private String tieBreakerColumn = "id";

        @Override
        protected SortField resolveSortField() {
            return sortField;
        }

        @Override
        protected SortOrderEnum resolveSortOrder() {
            return sortOrder != null ? sortOrder : super.resolveSortOrder();
        }

        @Override
        protected String tieBreaker() {
            return tieBreakerColumn;
        }
    }

    @Test
    @DisplayName("默认排序：排序键在前，稳定次序键兜底")
    void toPage_defaultSort_appendsSortKeyThenTieBreaker() {
        TestPageParam param = new TestPageParam();
        param.setPageNum(2);
        param.setPageSize(20);

        Page<Object> page = param.toPage();

        assertThat(page.getCurrent()).isEqualTo(2);
        assertThat(page.getSize()).isEqualTo(20);
        assertThat(page.orders()).extracting(OrderItem::getColumn, OrderItem::isAsc).containsExactly(
            org.assertj.core.api.Assertions.tuple("create_time", false),
            org.assertj.core.api.Assertions.tuple("id", false));
    }

    @Test
    @DisplayName("排序键缺少稳定次序键时，同值行会跨页重复，故 tie-breaker 必须始终存在")
    void toPage_anySortField_alwaysCarriesTieBreaker() {
        for (TestSortField field : TestSortField.values()) {
            TestPageParam param = new TestPageParam();
            param.setSortField(field);

            List<OrderItem> orders = param.toPage().orders();

            assertThat(orders).as("排序键 %s 缺少稳定次序键", field).isNotEmpty();
            assertThat(orders.getLast().getColumn()).isEqualTo("id");
        }
    }

    @Test
    @DisplayName("排序方向未指定时取排序键自身的默认方向")
    void resolveSortOrder_notProvided_followsSortFieldDefault() {
        TestPageParam param = new TestPageParam();
        param.setSortField(TestSortField.TITLE);

        assertThat(param.toPage().orders().getFirst().isAsc()).isTrue();
    }

    @Test
    @DisplayName("显式指定排序方向时覆盖排序键的默认方向")
    void resolveSortOrder_provided_overridesSortFieldDefault() {
        TestPageParam param = new TestPageParam();
        param.setSortField(TestSortField.CREATE_TIME);
        param.setSortOrder(SortOrderEnum.ASC);

        assertThat(param.toPage().orders().getFirst().isAsc()).isTrue();
    }

    @Test
    @DisplayName("排序键与稳定次序键同列时不重复追加")
    void toPage_sortColumnEqualsTieBreaker_doesNotDuplicateOrder() {
        TestPageParam param = new TestPageParam();
        param.setSortField(TestSortField.ID);

        assertThat(param.toPage().orders()).extracting(OrderItem::getColumn).containsExactly("id");
    }

    @Test
    @DisplayName("join 查询覆写的带别名次序键原样生效")
    void toPage_aliasQualifiedTieBreaker_isUsedAsIs() {
        TestPageParam param = new TestPageParam();
        param.setTieBreakerColumn("a.id");

        assertThat(param.toPage().orders()).extracting(OrderItem::getColumn).containsExactly("create_time", "a.id");
    }

    @Test
    @DisplayName("分页大小上限与分页插件的 maxLimit 取同一常量")
    void maxPageSize_isTheSingleSourceForPaginationGuard() {
        assertThat(BasePage.MAX_PAGE_SIZE).isEqualTo(100);
        assertThat(BasePage.DEFAULT_PAGE_SIZE).isLessThanOrEqualTo(BasePage.MAX_PAGE_SIZE);
    }
}
