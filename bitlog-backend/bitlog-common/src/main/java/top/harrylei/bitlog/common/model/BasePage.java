package top.harrylei.bitlog.common.model;

import com.baomidou.mybatisplus.core.metadata.OrderItem;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotNull;
import lombok.Data;
import top.harrylei.bitlog.common.enums.SortOrderEnum;

import java.io.Serial;
import java.io.Serializable;

/**
 * 分页基础类
 * <p>
 * 每个分页查询都必须回答「按什么排序」，不允许把 ORDER BY 散落在 DAO 或 XML 里。 子类声明 sortField/sortOrder 字段即开放给调用方选择，不声明则排序固定
 *
 * @author Harry
 * @since 2026-03-17
 */
@Data
public abstract class BasePage implements Serializable {

    @Serial
    private static final long serialVersionUID = 1L;

    public static final int DEFAULT_PAGE_NUM = 1;
    public static final int DEFAULT_PAGE_SIZE = 10;
    public static final int MAX_PAGE_SIZE = 100;

    private static final String DEFAULT_TIE_BREAKER = "id";

    @NotNull(message = "页码不能为空")
    @Min(value = 1, message = "页码最小为1")
    private Integer pageNum = DEFAULT_PAGE_NUM;

    @NotNull(message = "每页大小不能为空")
    @Min(value = 1, message = "每页大小最小为1")
    @Max(value = MAX_PAGE_SIZE, message = "每页大小最大为100")
    private Integer pageSize = DEFAULT_PAGE_SIZE;

    /** 生效的排序键 */
    protected abstract SortField resolveSortField();

    /** 生效的排序方向，默认取排序键自身的方向 */
    protected SortOrderEnum resolveSortOrder() {
        return resolveSortField().getDefaultOrder();
    }

    /** 稳定次序键，join 查询必须覆写为带表别名的形式 */
    protected String tieBreaker() {
        return DEFAULT_TIE_BREAKER;
    }

    /**
     * 构造分页对象，排序键在前、稳定次序键兜底
     * <p>
     * 分页插件会把 orders 改写进 SQL，Wrapper 与 XML 两条查询路径共用这一套排序
     */
    public <T> Page<T> toPage() {
        Page<T> page = Page.of(getPageNum(), getPageSize());
        String column = resolveSortField().getColumn();
        page.addOrder(resolveSortOrder() == SortOrderEnum.ASC ? OrderItem.asc(column) : OrderItem.desc(column));
        String tieBreaker = tieBreaker();
        if (!tieBreaker.equals(column)) {
            page.addOrder(OrderItem.desc(tieBreaker));
        }
        return page;
    }
}
