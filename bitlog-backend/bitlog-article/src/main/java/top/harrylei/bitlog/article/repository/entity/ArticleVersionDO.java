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
 * 文章版本实体（只追加，无 updateTime、deleted）
 *
 * @author Harry
 * @since 2026-04-09
 */
@Data
@Accessors(chain = true)
@TableName("article_version")
public class ArticleVersionDO implements Serializable {

    @Serial
    private static final long serialVersionUID = 1L;

    @TableId(type = IdType.AUTO)
    private Long id;

    /**
     * 文章 ID（逻辑关联 article.id）
     */
    private Long articleId;

    /**
     * 版本号
     */
    private Integer version;

    /**
     * 文章标题
     */
    private String title;

    /**
     * 文章正文内容
     */
    private String content;

    /**
     * 创建时间
     */
    private LocalDateTime createTime;
}
