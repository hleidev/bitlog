package top.harrylei.bitlog.comment.model.query;

import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Data;
import lombok.EqualsAndHashCode;
import lombok.experimental.Accessors;
import top.harrylei.bitlog.comment.model.enums.CommentSortEnum;
import top.harrylei.bitlog.common.model.BasePage;
import top.harrylei.bitlog.common.model.SortField;

/**
 * 文章评论列表查询参数
 *
 * @author Harry
 * @since 2026-08-06
 */
@Data
@EqualsAndHashCode(callSuper = true)
@Accessors(chain = true)
@Schema(description = "文章评论列表查询参数")
public class CommentPageParam extends BasePage {

    @Override
    protected SortField resolveSortField() {
        return CommentSortEnum.CREATE_TIME;
    }
}
