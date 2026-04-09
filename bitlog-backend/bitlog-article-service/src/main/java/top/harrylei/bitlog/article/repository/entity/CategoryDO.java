package top.harrylei.bitlog.article.repository.entity;

import com.baomidou.mybatisplus.annotation.TableName;
import lombok.Data;
import lombok.EqualsAndHashCode;
import lombok.experimental.Accessors;
import top.harrylei.bitlog.common.enums.DeleteStatusEnum;
import top.harrylei.bitlog.common.model.BaseDO;

import java.io.Serial;

/**
 * 文章分类实体
 *
 * @author harry
 * @since 0.0.1
 */
@Data
@EqualsAndHashCode(callSuper = true)
@Accessors(chain = true)
@TableName("category")
public class CategoryDO extends BaseDO {

    @Serial
    private static final long serialVersionUID = 1L;

    /**
     * 分类名称
     */
    private String name;

    /**
     * 分类描述
     */
    private String description;

    /**
     * 排序权重，越大越靠前
     */
    private Integer sortOrder;

    /** 关联文章数 */
    private Integer articleCount;

    /**
     * 是否删除：0-未删除，1-已删除
     */
    private DeleteStatusEnum deleted;
}
