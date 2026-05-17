package top.harrylei.bitlog.api.model.article.query;

import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.Size;
import lombok.Data;
import lombok.EqualsAndHashCode;
import lombok.experimental.Accessors;
import top.harrylei.bitlog.api.enums.article.ArticleStatusEnum;
import top.harrylei.bitlog.common.model.BasePage;

import java.util.List;

/**
 * 文章列表查询参数
 *
 * @author Harry
 * @since 2026-04-09
 */
@Data
@EqualsAndHashCode(callSuper = true)
@Accessors(chain = true)
@Schema(description = "文章列表查询参数")
public class ArticlePageQuery extends BasePage {

    @Schema(description = "分类 ID")
    private Long categoryId;

    @Size(max = 50, message = "关键词长度不能超过 50")
    @Schema(description = "关键词（搜索标题）")
    private String keyword;

    @Schema(description = "文章状态：草稿/已发布")
    private ArticleStatusEnum status;

    @Schema(description = "作者用户 ID")
    private Long userId;

    @Schema(description = "标签 ID 列表（AND 逻辑，文章需同时包含所有选中标签）")
    private List<Long> tagIds;
}
