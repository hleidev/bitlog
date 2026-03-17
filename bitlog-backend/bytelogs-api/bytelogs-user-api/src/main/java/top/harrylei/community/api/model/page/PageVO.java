package top.harrylei.community.api.model.page;

import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Data;

import java.io.Serial;
import java.io.Serializable;
import java.util.List;

/**
 * 统一分页结果类
 *
 * @author harry
 * @since 0.0.1
 */
@Data
@Schema(description = "分页结果")
public class PageVO<T> implements Serializable {

    @Serial
    private static final long serialVersionUID = 1L;

    @Schema(description = "当前页码，从1开始")
    private long pageNum;

    @Schema(description = "每页大小")
    private long pageSize;

    @Schema(description = "总页数")
    private long totalPages;

    @Schema(description = "总记录数")
    private long totalElements;

    @Schema(description = "是否有上一页")
    private boolean hasPrevious;

    @Schema(description = "是否有下一页")
    private boolean hasNext;

    @Schema(description = "当前页数据列表")
    private List<T> content;
}
