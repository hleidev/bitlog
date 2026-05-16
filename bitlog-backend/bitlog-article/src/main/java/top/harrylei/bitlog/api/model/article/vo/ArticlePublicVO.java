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

    @Schema(description = "封面图地址")
    private String cover;

    @Schema(description = "分类名称")
    private String categoryName;

    @Schema(description = "标签名称列表")
    private List<String> tags;

    @Schema(description = "是否置顶：0-否，1-是")
    private Integer topping;

    @Schema(description = "发布时间")
    private LocalDateTime publishTime;

    @Schema(description = "阅读数")
    private Integer readCount;

    @Schema(description = "评论数")
    private Integer commentCount;
}
