package top.harrylei.bitlog.api.model.article.dto;

import lombok.Data;
import lombok.experimental.Accessors;

import java.time.LocalDateTime;

/**
 * 文章内部服务间传输对象
 *
 * @author harry
 * @since 0.0.1
 */
@Data
@Accessors(chain = true)
public class ArticleDTO {

    /**
     * 文章 ID（article.id）
     */
    private Long id;

    /**
     * 作者用户 ID
     */
    private Long userId;

    /**
     * 文章标题
     */
    private String title;

    /**
     * 封面图地址
     */
    private String cover;

    /**
     * 文章摘要
     */
    private String summary;

    /**
     * 分类 ID
     */
    private Long categoryId;

    /**
     * 是否置顶：0-否，1-是
     */
    private Integer topping;

    /**
     * 发布时间
     */
    private LocalDateTime publishTime;

    /**
     * 阅读数
     */
    private Integer readCount;

    /**
     * 评论数
     */
    private Integer commentCount;
}
