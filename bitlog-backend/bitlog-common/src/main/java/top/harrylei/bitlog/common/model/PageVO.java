package top.harrylei.bitlog.common.model;

import com.baomidou.mybatisplus.core.metadata.IPage;
import lombok.Data;

import java.util.List;

/**
 * 统一分页结果类
 *
 * @param <T>
 *            数据类型
 * @author Harry
 * @since 2026-03-17
 */
@Data
public class PageVO<T> {

    private long pageNum;

    private long pageSize;

    private long totalPages;

    private long totalElements;

    private boolean hasPrevious;

    private boolean hasNext;

    private List<T> content;

    public static <T> PageVO<T> of(IPage<?> page, List<T> content) {
        PageVO<T> vo = new PageVO<>();
        vo.setPageNum(page.getCurrent());
        vo.setPageSize(page.getSize());
        vo.setTotalElements(page.getTotal());
        vo.setTotalPages(page.getPages());
        vo.setHasPrevious(page.getCurrent() > 1);
        vo.setHasNext(page.getCurrent() < page.getPages());
        vo.setContent(content);
        return vo;
    }
}
