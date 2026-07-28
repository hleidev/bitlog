package top.harrylei.bitlog.comment.repository.dao;

import com.baomidou.mybatisplus.core.metadata.IPage;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.baomidou.mybatisplus.extension.service.impl.ServiceImpl;
import org.springframework.stereotype.Repository;
import org.springframework.util.StringUtils;
import top.harrylei.bitlog.api.enums.comment.CommentStatusEnum;
import top.harrylei.bitlog.api.model.comment.query.CommentAdminPageParam;
import top.harrylei.bitlog.comment.repository.entity.CommentDO;
import top.harrylei.bitlog.comment.repository.mapper.CommentMapper;
import top.harrylei.bitlog.common.enums.DeleteStatusEnum;

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

    /**
     * 查询未删除的评论
     *
     * @param commentId 评论 ID
     * @return 评论实体，不存在或已删除时返回 null
     */
    public CommentDO getByIdAndNotDeleted(Long commentId) {
        if (commentId == null) {
            return null;
        }
        return lambdaQuery().eq(CommentDO::getId, commentId).eq(CommentDO::getDeleted, DeleteStatusEnum.NOT_DELETED)
            .one();
    }

    /**
     * 批量查询未删除的评论
     *
     * @param commentIds 评论 ID 集合
     * @return 评论列表
     */
    public List<CommentDO> listByIdsAndNotDeleted(Collection<Long> commentIds) {
        if (commentIds == null || commentIds.isEmpty()) {
            return List.of();
        }
        return lambdaQuery().in(CommentDO::getId, commentIds).eq(CommentDO::getDeleted, DeleteStatusEnum.NOT_DELETED)
            .list();
    }

    /**
     * 管理端分页查询，仅排除已删除评论
     *
     * @param param 查询条件
     * @param page 分页参数
     * @return 评论分页结果，按创建时间倒序
     */
    public IPage<CommentDO> pageForAdmin(CommentAdminPageParam param, Page<CommentDO> page) {
        return lambdaQuery().eq(CommentDO::getDeleted, DeleteStatusEnum.NOT_DELETED)
            .eq(param.getArticleId() != null, CommentDO::getArticleId, param.getArticleId())
            .eq(param.getUserId() != null, CommentDO::getUserId, param.getUserId())
            .eq(param.getStatus() != null, CommentDO::getStatus, param.getStatus())
            .like(StringUtils.hasText(param.getKeyword()), CommentDO::getContent, param.getKeyword())
            .orderByDesc(CommentDO::getCreateTime).page(page);
    }

    /**
     * 更新评论状态
     *
     * @param commentId 评论 ID
     * @param status 目标状态
     */
    public void updateStatus(Long commentId, CommentStatusEnum status) {
        lambdaUpdate().eq(CommentDO::getId, commentId).set(CommentDO::getStatus, status).update();
    }

    /**
     * 逻辑删除评论
     *
     * @param commentIds 评论 ID 集合
     */
    public void delete(Collection<Long> commentIds) {
        if (commentIds == null || commentIds.isEmpty()) {
            return;
        }
        lambdaUpdate().in(CommentDO::getId, commentIds).set(CommentDO::getDeleted, DeleteStatusEnum.DELETED).update();
    }
}
