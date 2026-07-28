package top.harrylei.bitlog.comment.repository.mapper;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import org.apache.ibatis.annotations.Mapper;
import top.harrylei.bitlog.comment.repository.entity.CommentDO;

/**
 * 评论 Mapper
 *
 * @author Harry
 * @since 2026-07-28
 */
@Mapper
public interface CommentMapper extends BaseMapper<CommentDO> {}
