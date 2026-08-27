package top.harrylei.bitlog.user.model.enums;

import lombok.AllArgsConstructor;
import lombok.Getter;
import top.harrylei.bitlog.common.enums.SortOrderEnum;
import top.harrylei.bitlog.common.model.SortField;

/**
 * 用户列表排序字段
 *
 * @author Harry
 * @since 2026-08-06
 */
@Getter
@AllArgsConstructor
public enum UserSortEnum implements SortField {

    /** 注册时间，列名带别名以避免 join 歧义 */
    CREATE_TIME("a.create_time", SortOrderEnum.DESC);

    private final String column;
    private final SortOrderEnum defaultOrder;
}
