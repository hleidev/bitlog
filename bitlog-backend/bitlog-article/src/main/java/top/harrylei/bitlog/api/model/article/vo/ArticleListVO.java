package top.harrylei.bitlog.api.model.article.vo;

import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Data;
import lombok.experimental.Accessors;
import top.harrylei.bitlog.common.model.PageVO;

/**
 * 文章管理列表响应（含状态计数和分页数据）
 *
 * @author Harry
 * @since 2026-05-03
 */
@Data
@Accessors(chain = true)
@Schema(description = "文章管理列表响应")
public class ArticleListVO {

    @Schema(description = "各状态文章数量统计")
    private ArticleCountVO counts;

    @Schema(description = "分页数据")
    private PageVO<ArticleVO> page;
}
