package top.harrylei.bitlog.link.model.enums;

import lombok.AllArgsConstructor;
import lombok.Getter;
import top.harrylei.bitlog.common.enums.SortOrderEnum;
import top.harrylei.bitlog.common.model.SortField;

/**
 * 友链列表排序字段
 *
 * @author Harry
 * @since 2026-08-15
 */
@Getter
@AllArgsConstructor
public enum FriendLinkSortEnum implements SortField {

    /** 提交时间 */
    CREATE_TIME("create_time", SortOrderEnum.DESC);

    private final String column;
    private final SortOrderEnum defaultOrder;
}
