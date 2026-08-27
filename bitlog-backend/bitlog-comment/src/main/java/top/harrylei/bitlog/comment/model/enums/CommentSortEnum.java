package top.harrylei.bitlog.comment.model.enums;

import lombok.AllArgsConstructor;
import lombok.Getter;
import top.harrylei.bitlog.common.enums.SortOrderEnum;
import top.harrylei.bitlog.common.model.SortField;

/**
 * 评论列表排序字段，读者端与管理端共用
 *
 * @author Harry
 * @since 2026-08-06
 */
@Getter
@AllArgsConstructor
public enum CommentSortEnum implements SortField {

    /** 发表时间 */
    CREATE_TIME("create_time", SortOrderEnum.DESC);

    private final String column;
    private final SortOrderEnum defaultOrder;
}
