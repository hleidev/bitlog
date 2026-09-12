package top.harrylei.bitlog.comment.repository.entity;

import com.baomidou.mybatisplus.annotation.TableName;
import java.io.Serial;
import lombok.Data;
import lombok.EqualsAndHashCode;
import lombok.experimental.Accessors;
import top.harrylei.bitlog.comment.model.enums.CommentStatusEnum;
import top.harrylei.bitlog.common.enums.DeleteStatusEnum;
import top.harrylei.bitlog.common.model.BaseDO;

/**
 * 评论实体
 *
 * @author Harry
 * @since 2026-07-28
 */
@Data
@EqualsAndHashCode(callSuper = true)
@Accessors(chain = true)
@TableName("comment")
public class CommentDO extends BaseDO {

    @Serial
    private static final long serialVersionUID = 1L;

    /**
     * 文章 ID（逻辑关联 article.id）
     */
    private Long articleId;

    /**
     * 评论者用户 ID（逻辑关联 user_account.id）
     */
    private Long userId;

    /**
     * 根评论 ID，null 表示自身即根评论
     */
    private Long rootId;

    /**
     * 父评论 ID，null 表示一级评论
     */
    private Long parentId;

    /**
     * 被回复者用户 ID，null 表示未指定被回复者（根评论或直接回复根评论）
     */
    private Long replyToUserId;

    /**
     * 评论内容（纯文本）
     */
    private String content;

    /**
     * 评论状态：正常/已隐藏
     */
    private CommentStatusEnum status;

    /**
     * 是否删除：0-未删除，1-已删除
     */
    private DeleteStatusEnum deleted;
}
