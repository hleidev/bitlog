package top.harrylei.bitlog.common.model;

import top.harrylei.bitlog.common.enums.SortOrderEnum;

/**
 * 排序字段白名单契约，实现类一律为枚举
 * <p>
 * 分页插件不校验 OrderItem 的列名，白名单固化在枚举里，非法值在 Spring 绑定阶段即被拒绝
 *
 * @author Harry
 * @since 2026-08-06
 */
public interface SortField {

    /** ORDER BY 列，join 查询必须带表别名 */
    String getColumn();

    /** 该字段的默认排序方向 */
    SortOrderEnum getDefaultOrder();
}
