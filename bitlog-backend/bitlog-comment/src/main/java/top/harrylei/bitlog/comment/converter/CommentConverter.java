package top.harrylei.bitlog.comment.converter;

import org.mapstruct.Mapper;
import org.mapstruct.Mapping;
import top.harrylei.bitlog.api.model.comment.vo.CommentAdminVO;
import top.harrylei.bitlog.api.model.comment.vo.CommentReplyVO;
import top.harrylei.bitlog.api.model.comment.vo.CommentUserVO;
import top.harrylei.bitlog.api.model.comment.vo.CommentVO;
import top.harrylei.bitlog.api.model.user.vo.UserVO;
import top.harrylei.bitlog.comment.repository.entity.CommentDO;

/**
 * 评论对象转换器
 *
 * @author Harry
 * @since 2026-07-28
 */
@Mapper(componentModel = "spring")
public interface CommentConverter {

    /**
     * CommentDO → CommentVO（作者、回复、墓碑标记由服务层填充）
     */
    @Mapping(target = "user", ignore = true)
    @Mapping(target = "removed", ignore = true)
    @Mapping(target = "replies", ignore = true)
    @Mapping(target = "replyCount", ignore = true)
    CommentVO toVO(CommentDO comment);

    /**
     * CommentDO → CommentReplyVO（作者与被回复者由服务层填充）
     */
    @Mapping(target = "user", ignore = true)
    @Mapping(target = "replyToUser", ignore = true)
    CommentReplyVO toReplyVO(CommentDO comment);

    /**
     * CommentDO → CommentAdminVO（作者与文章标题由服务层填充）
     */
    @Mapping(target = "user", ignore = true)
    @Mapping(target = "articleTitle", ignore = true)
    CommentAdminVO toAdminVO(CommentDO comment);

    /**
     * UserVO → CommentUserVO，仅保留评论区展示所需字段
     */
    CommentUserVO toCommentUser(UserVO user);
}
