package top.harrylei.bitlog.article.repository.entity;

import com.baomidou.mybatisplus.annotation.TableName;
import lombok.Data;
import lombok.EqualsAndHashCode;
import lombok.experimental.Accessors;
import top.harrylei.bitlog.common.enums.DeleteStatusEnum;
import top.harrylei.bitlog.common.model.BaseDO;

import java.io.Serial;
import java.time.LocalDateTime;

/**
 * 文章主表实体
 *
 * @author Harry
 * @since 2026-04-09
 */
@Data
@EqualsAndHashCode(callSuper = true)
@Accessors(chain = true)
@TableName("article")
public class ArticleDO extends BaseDO {

    @Serial
    private static final long serialVersionUID = 1L;

    /**
     * 作者用户 ID
     */
    private Long userId;

    /**
     * 最新版本 ID（逻辑关联 article_version.id），可为 null
     */
    private Long latestVersionId;

    /**
     * 已发布版本 ID，null 表示未发布（逻辑关联 article_version.id）
     */
    private Long publishedVersionId;

    /**
     * 首次发布时间，null 表示从未发布
     */
    private LocalDateTime publishTime;

    /**
     * 封面图地址
     */
    private String cover;

    /**
     * 文章摘要
     */
    private String summary;

    /**
     * 分类 ID（逻辑关联 category.id）
     */
    private Long categoryId;

    /**
     * 版本总数
     */
    private Integer versionCount;

    /**
     * 是否删除：0-未删除，1-已删除
     */
    private DeleteStatusEnum deleted;
}
