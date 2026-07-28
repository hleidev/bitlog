package top.harrylei.bitlog.api.enums.article;

/**
 * 文章列表排序方式
 *
 * @author Harry
 * @since 2026-07-28
 */
public enum ArticleSortEnum {

    /** 创建时间倒序 */
    CREATE_TIME,

    /** 发布时间倒序，纯草稿按创建时间参与排序 */
    PUBLISH_TIME
}
