package top.harrylei.bitlog.comment.repository.dao;

import com.baomidou.mybatisplus.core.conditions.update.LambdaUpdateWrapper;
import com.baomidou.mybatisplus.core.metadata.IPage;
import com.baomidou.mybatisplus.core.toolkit.Wrappers;
import com.baomidou.mybatisplus.extension.conditions.query.LambdaQueryChainWrapper;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.baomidou.mybatisplus.extension.service.impl.ServiceImpl;
import org.springframework.stereotype.Repository;
import org.springframework.util.StringUtils;
import top.harrylei.bitlog.comment.model.enums.CommentStatusEnum;
import top.harrylei.bitlog.comment.model.dto.CommentStatsDTO;
import top.harrylei.bitlog.comment.model.query.CommentAdminPageParam;
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
     * 分页查询文章的根评论，不过滤删除与隐藏状态，可见性由服务层判定（需要保留墓碑）
     *
     * @param articleId 文章 ID
     * @param page 分页参数，排序由 BasePage 提供
     * @return 根评论分页结果
     */
    public IPage<CommentDO> pageRootComments(Long articleId, Page<CommentDO> page) {
        return lambdaQuery().eq(CommentDO::getArticleId, articleId).isNull(CommentDO::getRootId).page(page);
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
     * @param page 分页参数，排序由 BasePage 提供
     * @return 评论分页结果
     */
    public IPage<CommentDO> pageForAdmin(CommentAdminPageParam param, Page<CommentDO> page) {
        return adminBaseQuery(param).eq(param.getStatus() != null, CommentDO::getStatus, param.getStatus())
            .page(page);
    }

    /**
     * 统计当前筛选下各状态的评论数量，供管理端 tab 计数使用
     * <p>
     * 与 pageForAdmin 共用 {@link #adminBaseQuery}，关键词口径必须一致， 否则搜索时 tab 数字会和列表行数对不上。
     * </p>
     *
     * @param param 查询条件，仅取关键词，状态分桶由本方法逐档统计
     * @return 状态计数
     */
    public CommentStatsDTO countStats(CommentAdminPageParam param) {
        CommentStatsDTO stats = new CommentStatsDTO();
        stats.setTotal(adminBaseQuery(param).count());
        stats.setVisible(adminBaseQuery(param).eq(CommentDO::getStatus, CommentStatusEnum.NORMAL).count());
        stats.setHidden(adminBaseQuery(param).eq(CommentDO::getStatus, CommentStatusEnum.HIDDEN).count());
        return stats;
    }

    /** 管理端列表与计数共用的基础过滤：排除已删除 + 关键词，不含状态 */
    private LambdaQueryChainWrapper<CommentDO> adminBaseQuery(CommentAdminPageParam param) {
        return lambdaQuery().eq(CommentDO::getDeleted, DeleteStatusEnum.NOT_DELETED)
            .like(StringUtils.hasText(param.getKeyword()), CommentDO::getContent, param.getKeyword());
    }

    /**
     * 条件更新评论状态，仅当评论未删除且当前状态为 from 时才生效。 返回是否真正发生了状态转移，调用方据此调整计数，避免并发下重复增减
     *
     * @param commentId 评论 ID
     * @param from 期望的当前状态
     * @param to 目标状态
     * @return true 状态已转移，false 未命中（已删除或已被他人改过）
     */
    public boolean updateStatus(Long commentId, CommentStatusEnum from, CommentStatusEnum to) {
        return lambdaUpdate().eq(CommentDO::getId, commentId).eq(CommentDO::getStatus, from)
            .eq(CommentDO::getDeleted, DeleteStatusEnum.NOT_DELETED).set(CommentDO::getStatus, to).update();
    }

    /**
     * 逻辑删除处于指定状态且尚未删除的评论，返回实际影响行数。 按状态分批是为了让调用方能据实际删除条数调整计数，而不是依赖读取时的快照
     *
     * @param commentIds 评论 ID 集合
     * @param status 仅删除处于该状态的评论
     * @return 实际被删除的条数
     */
    public int delete(Collection<Long> commentIds, CommentStatusEnum status) {
        if (commentIds == null || commentIds.isEmpty()) {
            return 0;
        }
        LambdaUpdateWrapper<CommentDO> wrapper = Wrappers.<CommentDO>lambdaUpdate().in(CommentDO::getId, commentIds)
            .eq(CommentDO::getStatus, status).eq(CommentDO::getDeleted, DeleteStatusEnum.NOT_DELETED)
            .set(CommentDO::getDeleted, DeleteStatusEnum.DELETED);
        return getBaseMapper().update(null, wrapper);
    }
}
