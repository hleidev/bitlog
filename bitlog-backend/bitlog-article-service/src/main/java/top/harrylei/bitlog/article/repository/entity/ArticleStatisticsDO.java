package top.harrylei.bitlog.article.repository.entity;

import com.baomidou.mybatisplus.annotation.TableName;
import lombok.Data;
import lombok.EqualsAndHashCode;
import lombok.experimental.Accessors;
import top.harrylei.bitlog.common.model.BaseDO;

import java.io.Serial;

/**
 * 文章统计实体
 *
 * @author harry
 * @since 0.0.1
 */
@Data
@EqualsAndHashCode(callSuper = true)
@Accessors(chain = true)
@TableName("article_statistics")
public class ArticleStatisticsDO extends BaseDO {

    @Serial
    private static final long serialVersionUID = 1L;

    /**
     * 文章 ID（逻辑关联 article.id）
     */
    private Long articleId;

    /**
     * 阅读数
     */
    private Integer readCount;

    /**
     * 评论数
     */
    private Integer commentCount;
}
