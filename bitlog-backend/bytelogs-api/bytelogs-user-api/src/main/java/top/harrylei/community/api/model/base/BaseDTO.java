package top.harrylei.community.api.model.base;

import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Data;
import lombok.experimental.Accessors;

import java.io.Serializable;
import java.time.LocalDateTime;

/**
 * 基础传输对象
 *
 * @author harry
 * @since 0.0.1
 */
@Data
@Accessors(chain = true)
public class BaseDTO implements Serializable {

    @Schema(description = "业务主键")
    private Long id;

    @Schema(description = "创建时间")
    private LocalDateTime createTime;

    @Schema(description = "最后编辑时间")
    private LocalDateTime updateTime;
}
