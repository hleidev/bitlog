package top.harrylei.bitlog.api.model.article.vo;

import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Data;
import lombok.experimental.Accessors;

import java.time.LocalDateTime;

/**
 * 标签视图对象
 *
 * @author harry
 * @since 0.0.1
 */
@Data
@Accessors(chain = true)
@Schema(description = "标签视图对象")
public class TagVO {
    @Schema(description = "标签 ID")
    private Long id;
    @Schema(description = "标签名称")
    private String name;
    @Schema(description = "关联文章数")
    private Integer articleCount;
    @Schema(description = "创建时间")
    private LocalDateTime createTime;
    @Schema(description = "最后修改时间")
    private LocalDateTime updateTime;
}
