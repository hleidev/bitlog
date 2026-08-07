package top.harrylei.bitlog.server;

import com.baomidou.mybatisplus.core.metadata.OrderItem;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.config.BeanDefinition;
import org.springframework.context.annotation.ClassPathScanningCandidateComponentProvider;
import org.springframework.core.type.filter.AssignableTypeFilter;
import top.harrylei.bitlog.common.model.SortField;

import java.util.ArrayList;
import java.util.List;
import java.util.regex.Pattern;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * 排序字段契约测试，覆盖全模块所有 SortField 实现
 *
 * @author Harry
 * @since 2026-08-07
 */
class SortFieldContractTest {

    private static final String BASE_PACKAGE = "top.harrylei.bitlog";

    /** 允许 create_time 或 a.create_time，不允许函数、表达式、空格 */
    private static final Pattern IDENTIFIER = Pattern.compile("^[A-Za-z_][A-Za-z0-9_]*(\\.[A-Za-z_][A-Za-z0-9_]*)?$");

    private static List<SortField> allSortFields() {
        ClassPathScanningCandidateComponentProvider scanner = new ClassPathScanningCandidateComponentProvider(false) {
            @Override
            protected boolean isCandidateComponent(
                org.springframework.beans.factory.annotation.AnnotatedBeanDefinition beanDefinition) {
                return true;
            }
        };
        scanner.addIncludeFilter(new AssignableTypeFilter(SortField.class));

        List<SortField> fields = new ArrayList<>();
        for (BeanDefinition definition : scanner.findCandidateComponents(BASE_PACKAGE)) {
            try {
                Class<?> type = Class.forName(definition.getBeanClassName());
                if (type.isEnum()) {
                    for (Object constant : type.getEnumConstants()) {
                        fields.add((SortField)constant);
                    }
                }
            } catch (ClassNotFoundException e) {
                throw new IllegalStateException("无法加载排序枚举: " + definition.getBeanClassName(), e);
            }
        }
        return fields;
    }

    @Test
    @DisplayName("扫描得到的排序枚举覆盖全部业务模块")
    void allSortFields_scanned_coversEveryModule() {
        List<String> names = allSortFields().stream().map(f -> ((Enum<?>)f).getDeclaringClass().getSimpleName())
            .distinct().sorted().toList();

        assertThat(names).contains("ArticleSortEnum", "MyArticleSortEnum", "UserSortEnum", "CommentSortEnum");
    }

    /**
     * OrderItem.setColumn 内部调用 StringUtils.replaceAllBlank，表达式里的空格会在构造时被抹掉， 生成语法错误的 SQL 且不报错。排序列因此只能是标识符，派生值必须在 SELECT
     * 里取别名
     */
    @Test
    @DisplayName("排序列必须是标识符，表达式会被 OrderItem 抹掉空格")
    void getColumn_everySortField_isPlainIdentifier() {
        for (SortField field : allSortFields()) {
            String column = field.getColumn();
            String name = ((Enum<?>)field).getDeclaringClass().getSimpleName() + "." + ((Enum<?>)field).name();

            assertThat(column).as("%s 的排序列为空", name).isNotBlank();
            assertThat(column).as("%s 的排序列含空白字符，OrderItem 会将其抹掉并生成非法 SQL", name).doesNotContainAnyWhitespaces();
            assertThat(IDENTIFIER.matcher(column).matches())
                .as("%s 的排序列 [%s] 不是合法标识符，派生值请在 SELECT 中取别名后再排序", name, column).isTrue();
        }
    }

    @Test
    @DisplayName("排序列经 OrderItem 往返后必须一字不变")
    void getColumn_everySortField_survivesOrderItemUnchanged() {
        for (SortField field : allSortFields()) {
            String column = field.getColumn();
            String name = ((Enum<?>)field).getDeclaringClass().getSimpleName() + "." + ((Enum<?>)field).name();

            assertThat(OrderItem.desc(column).getColumn()).as("%s 的排序列被 OrderItem 改写了", name).isEqualTo(column);
        }
    }

    @Test
    @DisplayName("每个排序字段都要声明默认方向")
    void getDefaultOrder_everySortField_isDeclared() {
        for (SortField field : allSortFields()) {
            assertThat(field.getDefaultOrder()).as("%s 未声明默认排序方向", ((Enum<?>)field).name()).isNotNull();
        }
    }
}
