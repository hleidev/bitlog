package top.harrylei.bitlog.article.repository.entity;

import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import lombok.Data;
import lombok.experimental.Accessors;

import java.io.Serial;
import java.io.Serializable;
import java.time.LocalDateTime;

/**
 * 文章标签关联实体
 *
 * @author harry
 * @since 0.0.1
 */
@Data
@Accessors(chain = true)
@TableName("article_tag")
public class ArticleTagDO implements Serializable {

    @Serial
    private static final long serialVersionUID = 1L;

    @TableId(type = IdType.AUTO)
    private Long id;

    /**
     * 文章 ID
     */
    private Long articleId;

    /**
     * 标签 ID
     */
    private Long tagId;

    /**
     * 创建时间
     */
    private LocalDateTime createTime;
}
