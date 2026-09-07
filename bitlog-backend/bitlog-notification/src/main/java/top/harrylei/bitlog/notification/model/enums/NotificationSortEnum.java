package top.harrylei.bitlog.notification.model.enums;

import lombok.AllArgsConstructor;
import lombok.Getter;
import top.harrylei.bitlog.common.enums.SortOrderEnum;
import top.harrylei.bitlog.common.model.SortField;

/**
 * 通知列表排序字段
 *
 * @author Harry
 * @since 2026-09-07
 */
@Getter
@AllArgsConstructor
public enum NotificationSortEnum implements SortField {

    /** 发生时间 */
    CREATE_TIME("create_time", SortOrderEnum.DESC);

    private final String column;
    private final SortOrderEnum defaultOrder;
}
