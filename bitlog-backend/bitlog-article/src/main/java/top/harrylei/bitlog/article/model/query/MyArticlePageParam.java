package top.harrylei.bitlog.article.model.query;

import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.Size;
import lombok.Data;
import lombok.EqualsAndHashCode;
import lombok.experimental.Accessors;
import top.harrylei.bitlog.article.model.enums.ArticleStatusEnum;
import top.harrylei.bitlog.article.model.enums.MyArticleSortEnum;
import top.harrylei.bitlog.common.enums.SortOrderEnum;
import top.harrylei.bitlog.common.model.BasePage;
import top.harrylei.bitlog.common.model.SortField;

/**
 * 我的文章列表查询参数
 *
 * @author Harry
 * @since 2026-08-06
 */
@Data
@EqualsAndHashCode(callSuper = true)
@Accessors(chain = true)
@Schema(description = "我的文章列表查询参数")
public class MyArticlePageParam extends BasePage {

    @Size(max = 50, message = "关键词长度不能超过 50")
    @Schema(description = "关键词（搜索标题与摘要）")
    private String keyword;

    @Schema(description = "文章状态：0-草稿，1-已发布")
    private ArticleStatusEnum status;

    @Schema(description = "排序字段：CREATE_TIME-创建时间（默认），UPDATE_TIME-更新时间，DISPLAY_TIME-列表展示时间")
    private MyArticleSortEnum sortField;

    @Schema(description = "排序方向：ASC / DESC，不传取排序字段的默认方向")
    private SortOrderEnum sortOrder;

    @Override
    protected SortField resolveSortField() {
        return sortField != null ? sortField : MyArticleSortEnum.CREATE_TIME;
    }

    @Override
    protected SortOrderEnum resolveSortOrder() {
        return sortOrder != null ? sortOrder : super.resolveSortOrder();
    }

    @Override
    protected String tieBreaker() {
        return "a.id";
    }

    /** status 翻译：true 只看已发布，false 只看草稿，null 不过滤；避免 XML 里做 OGNL 字符串比较 */
    @Schema(hidden = true)
    public Boolean getPublishedOnly() {
        return status == null ? null : ArticleStatusEnum.PUBLISHED == status;
    }
}
