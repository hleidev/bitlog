package top.harrylei.bitlog.api.model.article.vo;

import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Data;
import lombok.experimental.Accessors;

import java.time.LocalDateTime;
import java.util.List;

/**
 * 文章公开列表视图对象（公开接口使用，不含内部版本/状态等字段）
 *
 * @author Harry
 * @since 2026-05-16
 */
@Data
@Accessors(chain = true)
@Schema(description = "文章公开列表视图对象")
public class ArticlePublicVO {

    @Schema(description = "文章 ID")
    private Long id;

    @Schema(description = "文章标题")
    private String title;

    @Schema(description = "文章摘要")
    private String summary;

    @Schema(description = "所属分类")
    private CategoryVO category;

    @Schema(description = "标签列表")
    private List<TagVO> tags;

    @Schema(description = "发布时间")
    private LocalDateTime publishTime;
}
