package top.harrylei.bitlog.comment.service;

import top.harrylei.bitlog.api.enums.comment.CommentStatusEnum;
import top.harrylei.bitlog.api.model.comment.query.CommentAdminPageParam;
import top.harrylei.bitlog.api.model.comment.req.CommentSaveParam;
import top.harrylei.bitlog.api.model.comment.vo.CommentAdminVO;
import top.harrylei.bitlog.api.model.comment.vo.CommentStatsVO;
import top.harrylei.bitlog.api.model.comment.vo.CommentVO;
import top.harrylei.bitlog.api.model.comment.query.CommentPageParam;
import top.harrylei.bitlog.common.model.PageVO;

import java.util.List;

/**
 * 评论业务服务接口
 *
 * @author Harry
 * @since 2026-07-28
 */
public interface CommentService {

    /**
     * 分页查询文章评论，含每条根评论下的可见回复
     *
     * @param articleId 文章 ID
     * @param page 分页参数
     * @return 根评论分页结果
     */
    PageVO<CommentVO> pageComments(Long articleId, CommentPageParam query);

    /**
     * 发表评论或回复
     *
     * @param userId 评论者用户 ID
     * @param articleId 文章 ID
     * @param req 发表请求
     * @return 新建评论 ID
     */
    Long saveComment(Long userId, Long articleId, CommentSaveParam req);

    /**
     * 删除本人评论
     *
     * @param userId 操作者用户 ID
     * @param articleId 文章 ID
     * @param commentId 评论 ID
     */
    void deleteComment(Long userId, Long articleId, Long commentId);

    /**
     * 管理端分页查询全站评论
     *
     * @param req 查询条件
     * @return 评论分页结果
     */
    PageVO<CommentAdminVO> pageForAdmin(CommentAdminPageParam req);

    /**
     * 管理端统计各状态的评论数量
     *
     * @return 状态计数
     */
    CommentStatsVO getCommentStats(CommentAdminPageParam req);

    /**
     * 更新评论状态（隐藏或恢复）
     *
     * @param commentId 评论 ID
     * @param status 目标状态
     */
    void updateStatus(Long commentId, CommentStatusEnum status);

    /**
     * 批量删除评论
     *
     * @param commentIds 评论 ID 列表
     */
    void batchDelete(List<Long> commentIds);
}
