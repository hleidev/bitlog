package top.harrylei.bitlog.common.model;

import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableId;
import lombok.Data;
import lombok.experimental.Accessors;

import java.io.Serializable;
import java.time.OffsetDateTime;

/**
 * 基础数据对象
 *
 * @author Harry
 * @since 2026-03-17
 */
@Data
@Accessors(chain = true)
public class BaseDO implements Serializable {

    @TableId(type = IdType.AUTO)
    private Long id;

    private OffsetDateTime createTime;

    private OffsetDateTime updateTime;
}
