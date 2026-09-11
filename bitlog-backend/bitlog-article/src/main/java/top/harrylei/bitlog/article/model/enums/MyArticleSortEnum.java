package top.harrylei.bitlog.article.model.enums;

import lombok.AllArgsConstructor;
import lombok.Getter;
import top.harrylei.bitlog.common.enums.SortOrderEnum;
import top.harrylei.bitlog.common.model.SortField;

/**
 * 我的文章列表排序字段
 *
 * @author Harry
 * @since 2026-08-06
 */
@Getter
@AllArgsConstructor
public enum MyArticleSortEnum implements SortField {

    /** 创建时间 */
    CREATE_TIME("a.create_time", SortOrderEnum.DESC),

    /** 最近更新：用于工作台继续编辑，仍以文章 ID 作为稳定分页的次排序键 */
    UPDATE_TIME("a.update_time", SortOrderEnum.DESC),

    /**
     * 列表展示时间：未发布（含下架）按创建时间，已发布按发布时间
     * <p>
     * 派生列由 ArticleMapper.pageByUser 的 SELECT 产出，排序列必须是标识符，OrderItem 会抹掉表达式里的空格
     */
    DISPLAY_TIME("display_time", SortOrderEnum.DESC);

    private final String column;
    private final SortOrderEnum defaultOrder;
}
