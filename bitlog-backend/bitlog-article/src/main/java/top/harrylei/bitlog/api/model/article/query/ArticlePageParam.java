package top.harrylei.bitlog.api.model.article.query;

import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.Size;
import lombok.Data;
import lombok.EqualsAndHashCode;
import lombok.experimental.Accessors;
import top.harrylei.bitlog.api.enums.article.ArticleSortEnum;
import top.harrylei.bitlog.common.model.BasePage;
import top.harrylei.bitlog.common.model.SortField;

import java.util.List;

/**
 * 公开文章列表查询参数
 *
 * @author Harry
 * @since 2026-04-09
 */
@Data
@EqualsAndHashCode(callSuper = true)
@Accessors(chain = true)
@Schema(description = "公开文章列表查询参数")
public class ArticlePageParam extends BasePage {

    @Schema(description = "分类 ID")
    private Long categoryId;

    @Size(max = 50, message = "关键词长度不能超过 50")
    @Schema(description = "关键词（搜索标题与摘要）")
    private String keyword;

    @Size(max = 10, message = "标签数量不能超过 10")
    @Schema(description = "标签 ID 列表，AND 语义：文章需同时包含所有选中标签")
    private List<Long> allTagIds;

    @Override
    protected SortField resolveSortField() {
        return ArticleSortEnum.PUBLISH_TIME;
    }

    @Override
    protected String tieBreaker() {
        return "a.id";
    }
}
