package top.harrylei.bitlog.common.model;

import lombok.Data;

import java.io.Serializable;
import java.time.LocalDateTime;

/**
 * VO 基类
 *
 * @author Harry
 * @since 2026-03-17
 */
@Data
public class BaseVO implements Serializable {

    private Long id;

    private LocalDateTime createTime;

    private LocalDateTime updateTime;
}
