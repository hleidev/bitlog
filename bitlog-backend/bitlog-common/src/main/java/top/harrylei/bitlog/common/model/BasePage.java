package top.harrylei.bitlog.common.model;

import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotNull;
import lombok.Data;

import java.io.Serial;
import java.io.Serializable;

/**
 * 分页基础类
 *
 * @author Harry
 * @since 2026-03-17
 */
@Data
public class BasePage implements Serializable {

    @Serial
    private static final long serialVersionUID = 1L;

    public static final int DEFAULT_PAGE_NUM = 1;
    public static final int DEFAULT_PAGE_SIZE = 10;
    public static final int MAX_PAGE_SIZE = 100;

    @NotNull(message = "页码不能为空")
    @Min(value = 1, message = "页码最小为1")
    private Integer pageNum = DEFAULT_PAGE_NUM;

    @NotNull(message = "每页大小不能为空")
    @Min(value = 1, message = "每页大小最小为1")
    @Max(value = MAX_PAGE_SIZE, message = "每页大小最大为100")
    private Integer pageSize = DEFAULT_PAGE_SIZE;
}
