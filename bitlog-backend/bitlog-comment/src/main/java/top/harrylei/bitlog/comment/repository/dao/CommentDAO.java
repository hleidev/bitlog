package top.harrylei.bitlog.comment.repository.dao;

import com.baomidou.mybatisplus.core.metadata.IPage;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.baomidou.mybatisplus.extension.service.impl.ServiceImpl;
import org.springframework.stereotype.Repository;
import top.harrylei.bitlog.comment.repository.entity.CommentDO;
import top.harrylei.bitlog.comment.repository.mapper.CommentMapper;

import java.util.Collection;
import java.util.List;

/**
 * 评论数据访问对象
 *
 * @author Harry
 * @since 2026-07-28
 */
@Repository
public class CommentDAO extends ServiceImpl<CommentMapper, CommentDO> {

    /**
     * root_id 取该值表示评论自身即为根评论
     */
    public static final long ROOT_COMMENT_ID = 0L;

    /**
     * 分页查询文章的根评论，不过滤删除与隐藏状态，可见性由服务层判定（需要保留墓碑）
     *
     * @param articleId 文章 ID
     * @param page 分页参数
     * @return 根评论分页结果，按创建时间倒序
     */
    public IPage<CommentDO> pageRootComments(Long articleId, Page<CommentDO> page) {
        return lambdaQuery().eq(CommentDO::getArticleId, articleId).eq(CommentDO::getRootId, ROOT_COMMENT_ID)
            .orderByDesc(CommentDO::getCreateTime).page(page);
    }

    /**
     * 批量查询指定根评论下的全部回复，不过滤删除与隐藏状态，可见性由服务层判定
     *
     * @param rootIds 根评论 ID 集合
     * @return 回复列表，按创建时间正序
     */
    public List<CommentDO> listRepliesByRootIds(Collection<Long> rootIds) {
        if (rootIds == null || rootIds.isEmpty()) {
            return List.of();
        }
        return lambdaQuery().in(CommentDO::getRootId, rootIds).orderByAsc(CommentDO::getCreateTime).list();
    }
}
