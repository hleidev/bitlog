package top.harrylei.bitlog.api.model.article.vo;

import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Data;
import lombok.experimental.Accessors;
import top.harrylei.bitlog.api.enums.article.ArticleStatusEnum;

import java.time.OffsetDateTime;
import java.util.List;

/**
 * 文章列表视图对象（不含正文内容）
 *
 * @author Harry
 * @since 2026-04-09
 */
@Data
@Accessors(chain = true)
@Schema(description = "文章列表视图对象")
public class ArticleVO {

    @Schema(description = "文章 ID（article.id）")
    private Long id;

    @Schema(description = "作者用户 ID")
    private Long userId;

    @Schema(description = "文章标题")
    private String title;

    @Schema(description = "文章摘要")
    private String summary;

    @Schema(description = "文章状态")
    private ArticleStatusEnum status;

    @Schema(description = "最新草稿版本 ID（article_version.id）")
    private Long latestVersionId;

    @Schema(description = "已发布版本 ID（article_version.id），null 表示从未发布")
    private Long publishedVersionId;

    @Schema(description = "所属分类")
    private CategoryVO category;

    @Schema(description = "标签列表")
    private List<TagVO> tags;

    @Schema(description = "发布时间")
    private OffsetDateTime publishTime;

    @Schema(description = "创建时间")
    private OffsetDateTime createTime;

    @Schema(description = "更新时间")
    private OffsetDateTime updateTime;

    @Schema(description = "阅读数")
    private Integer readCount;

    @Schema(description = "评论数")
    private Integer commentCount;
}
