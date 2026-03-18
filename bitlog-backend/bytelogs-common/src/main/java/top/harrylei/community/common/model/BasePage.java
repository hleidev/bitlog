package top.harrylei.community.common.model;

import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotNull;
import lombok.Data;

import java.io.Serial;
import java.io.Serializable;
import java.util.Map;

/**
 * 分页基础类
 *
 * @author harry
 * @since 0.0.1
 */
@Data
public class BasePage implements Serializable {

    @Serial
    private static final long serialVersionUID = 1L;

    public static final int DEFAULT_PAGE_NUM = 1;
    public static final int DEFAULT_PAGE_SIZE = 10;
    public static final int MAX_PAGE_SIZE = 100;

    public static final Map<String, String> DEFAULT_SORT_MAPPING = Map.of(
            "createTime", "create_time"
    );

    @NotNull(message = "页码不能为空")
    @Min(value = 1, message = "页码最小为1")
    private Integer pageNum = DEFAULT_PAGE_NUM;

    @NotNull(message = "每页大小不能为空")
    @Min(value = 1, message = "每页大小最小为1")
    @Max(value = MAX_PAGE_SIZE, message = "每页大小最大为100")
    private Integer pageSize = DEFAULT_PAGE_SIZE;

    private String sortField;

    /**
     * 获取字段映射关系（子类可覆盖以扩展）
     */
    public Map<String, String> getFieldMapping() {
        return DEFAULT_SORT_MAPPING;
    }
}
