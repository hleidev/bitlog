package top.harrylei.bitlog.api.model.article.vo;

import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Data;
import lombok.EqualsAndHashCode;
import lombok.experimental.Accessors;

/**
 * 文章公开详情视图对象（读者视角，含正文，不含内部版本/状态等字段）
 *
 * @author Harry
 * @since 2026-05-16
 */
@Data
@EqualsAndHashCode(callSuper = true)
@Accessors(chain = true)
@Schema(description = "文章公开详情视图对象")
public class ArticlePublicDetailVO extends ArticlePublicVO {

    @Schema(description = "文章正文内容")
    private String content;
}
