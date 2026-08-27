package top.harrylei.bitlog.article.model.enums;

import lombok.AllArgsConstructor;
import lombok.Getter;
import top.harrylei.bitlog.common.enums.SortOrderEnum;
import top.harrylei.bitlog.common.model.SortField;

/**
 * 公开文章列表排序字段
 *
 * @author Harry
 * @since 2026-07-28
 */
@Getter
@AllArgsConstructor
public enum ArticleSortEnum implements SortField {

    /** 发布时间，带表别名以区分子查询中的同名列 */
    PUBLISH_TIME("a.publish_time", SortOrderEnum.DESC);

    private final String column;
    private final SortOrderEnum defaultOrder;
}
