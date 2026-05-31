package top.harrylei.bitlog.article.repository.entity;

import com.baomidou.mybatisplus.annotation.TableName;
import lombok.Data;
import lombok.EqualsAndHashCode;
import lombok.experimental.Accessors;
import top.harrylei.bitlog.common.model.BaseDO;

import java.io.Serial;

/**
 * 标签实体
 *
 * @author Harry
 * @since 2026-04-09
 */
@Data
@EqualsAndHashCode(callSuper = true)
@Accessors(chain = true)
@TableName("tag")
public class TagDO extends BaseDO {

    @Serial
    private static final long serialVersionUID = 1L;

    /**
     * 标签名称
     */
    private String name;
}
