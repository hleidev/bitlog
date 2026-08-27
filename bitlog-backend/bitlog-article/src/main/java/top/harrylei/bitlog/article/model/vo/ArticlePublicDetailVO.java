package top.harrylei.bitlog.article.model.vo;

import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Data;
import lombok.experimental.Accessors;

import java.time.OffsetDateTime;
import java.util.List;

/**
 * 文章公开详情视图对象（读者视角，含正文，不含内部版本/状态等字段）
 *
 * @author Harry
 * @since 2026-05-16
 */
@Data
@Accessors(chain = true)
@Schema(description = "文章公开详情视图对象")
public class ArticlePublicDetailVO {

    @Schema(description = "文章 ID")
    private Long id;

    @Schema(description = "作者用户 ID")
    private Long userId;

    @Schema(description = "文章标题")
    private String title;

    @Schema(description = "文章摘要")
    private String summary;

    @Schema(description = "文章正文内容")
    private String content;

    @Schema(description = "所属分类")
    private CategoryVO category;

    @Schema(description = "标签列表")
    private List<TagVO> tags;

    @Schema(description = "发布时间")
    private OffsetDateTime publishTime;
}
