package top.harrylei.bitlog.api.model.article.vo;

import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Data;
import lombok.experimental.Accessors;
import top.harrylei.bitlog.api.enums.article.ArticleStatusEnum;

import java.time.LocalDateTime;
import java.util.List;

/**
 * 文章列表视图对象（不含正文内容）
 *
 * @author harry
 * @since 0.0.1
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

    @Schema(description = "封面图地址")
    private String cover;

    @Schema(description = "文章摘要")
    private String summary;

    @Schema(description = "文章状态")
    private ArticleStatusEnum status;

    @Schema(description = "分类 ID")
    private Long categoryId;

    @Schema(description = "分类名称")
    private String categoryName;

    @Schema(description = "标签名称列表")
    private List<String> tags;

    @Schema(description = "是否置顶：0-否，1-是")
    private Integer topping;

    @Schema(description = "发布时间")
    private LocalDateTime publishTime;

    @Schema(description = "创建时间")
    private LocalDateTime createTime;

    @Schema(description = "更新时间")
    private LocalDateTime updateTime;

    @Schema(description = "阅读数")
    private Integer readCount;

    @Schema(description = "评论数")
    private Integer commentCount;
}
