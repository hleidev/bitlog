package top.harrylei.bitlog.common.model;

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

    private Long id;

    private LocalDateTime createTime;

    private LocalDateTime updateTime;
}
