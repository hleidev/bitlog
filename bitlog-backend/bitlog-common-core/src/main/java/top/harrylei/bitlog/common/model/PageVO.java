package top.harrylei.bitlog.common.model;

import lombok.Data;

import java.io.Serial;
import java.io.Serializable;
import java.util.List;

/**
 * 统一分页结果类
 *
 * @param <T> 数据类型
 * @author harry
 * @since 0.0.1
 */
@Data
public class PageVO<T> implements Serializable {

    @Serial
    private static final long serialVersionUID = 1L;

    private long pageNum;

    private long pageSize;

    private long totalPages;

    private long totalElements;

    private boolean hasPrevious;

    private boolean hasNext;

    private List<T> content;
}
