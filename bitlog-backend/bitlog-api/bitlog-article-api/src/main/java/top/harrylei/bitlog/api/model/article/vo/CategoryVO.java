package top.harrylei.bitlog.api.model.article.vo;

import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Data;
import lombok.experimental.Accessors;

import java.time.LocalDateTime;

/**
 * 分类视图对象
 *
 * @author harry
 * @since 0.0.1
 */
@Data
@Accessors(chain = true)
@Schema(description = "分类视图对象")
public class CategoryVO {
    @Schema(description = "分类 ID")
    private Long id;
    @Schema(description = "父分类 ID，0 表示顶级分类")
    private Long parentId;
    @Schema(description = "分类名称")
    private String name;
    @Schema(description = "关联文章数")
    private Integer articleCount;
    @Schema(description = "创建时间")
    private LocalDateTime createTime;
}
