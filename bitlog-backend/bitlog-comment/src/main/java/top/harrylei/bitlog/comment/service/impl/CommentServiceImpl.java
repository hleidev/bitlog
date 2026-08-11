package top.harrylei.bitlog.comment.service.impl;

import com.baomidou.mybatisplus.core.metadata.IPage;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import top.harrylei.bitlog.api.enums.comment.CommentStatusEnum;
import top.harrylei.bitlog.api.model.comment.query.CommentAdminPageParam;
import top.harrylei.bitlog.api.model.comment.query.CommentPageParam;
import top.harrylei.bitlog.api.model.comment.req.CommentSaveParam;
import top.harrylei.bitlog.api.model.comment.vo.CommentAdminVO;
import top.harrylei.bitlog.api.model.comment.vo.CommentReplyVO;
import top.harrylei.bitlog.api.model.comment.vo.CommentUserVO;
import top.harrylei.bitlog.api.model.comment.vo.CommentVO;
import top.harrylei.bitlog.api.model.user.vo.UserVO;
import top.harrylei.bitlog.article.port.ArticlePort;
import top.harrylei.bitlog.comment.config.CommentProperties;
import top.harrylei.bitlog.comment.converter.CommentConverter;
import top.harrylei.bitlog.comment.repository.dao.CommentDAO;
import top.harrylei.bitlog.comment.repository.entity.CommentDO;
import top.harrylei.bitlog.comment.service.CommentService;
import top.harrylei.bitlog.common.constants.RedisKeyConstants;
import top.harrylei.bitlog.common.context.ReqInfoContext;
import top.harrylei.bitlog.common.enums.DeleteStatusEnum;
import top.harrylei.bitlog.common.enums.ResultCode;
import top.harrylei.bitlog.common.exception.BusinessException;
import top.harrylei.bitlog.common.model.PageVO;
import top.harrylei.bitlog.common.util.RateLimiter;
import top.harrylei.bitlog.user.port.UserPort;

import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.stream.Collectors;

/**
 * 评论业务服务实现
 *
 * @author Harry
 * @since 2026-07-28
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class CommentServiceImpl implements CommentService {

    /**
     * root_id / parent_id / reply_to_user_id 的空值约定
     */
    private static final long NONE = 0L;

    /**
     * 最小间隔窗口内只允许一条，故配额固定为 1
     */
    private static final int INTERVAL_QUOTA = 1;

    private final CommentDAO commentDAO;
    private final CommentConverter commentConverter;
    private final ArticlePort articlePort;
    private final UserPort userPort;
    private final RateLimiter rateLimiter;
    private final CommentProperties commentProperties;

    @Override
    public PageVO<CommentVO> pageComments(Long articleId, CommentPageParam query) {
        if (!articlePort.isPublished(articleId)) {
            throw ResultCode.ARTICLE_NOT_PUBLISHED.toException();
        }

        IPage<CommentDO> rootPage = commentDAO.pageRootComments(articleId, query.toPage());
        List<CommentDO> roots = rootPage.getRecords();
        if (roots.isEmpty()) {
            return PageVO.of(rootPage, List.of());
        }

        Map<Long, List<CommentDO>> repliesByRoot =
            commentDAO.listRepliesByRootIds(roots.stream().map(CommentDO::getId).toList()).stream()
                .filter(this::isVisible).collect(Collectors.groupingBy(CommentDO::getRootId));

        // 不可见且无可见回复的根评论整条丢弃，其余保留：不可见的渲染为墓碑，托住整楼回复
        List<CommentDO> rendered = roots.stream()
            .filter(root -> isVisible(root) || !repliesByRoot.getOrDefault(root.getId(), List.of()).isEmpty()).toList();

        Map<Long, CommentUserVO> userMap = loadUsers(rendered, repliesByRoot);
        List<CommentVO> content = rendered.stream()
            .map(root -> buildRootVO(root, repliesByRoot.getOrDefault(root.getId(), List.of()), userMap)).toList();
        return PageVO.of(rootPage, content);
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public Long saveComment(Long userId, Long articleId, CommentSaveParam req) {
        if (!articlePort.isPublished(articleId)) {
            throw ResultCode.COMMENT_NOT_ALLOWED.toException();
        }
        checkRateLimit(userId);

        CommentDO comment = new CommentDO().setArticleId(articleId).setUserId(userId)
            .setContent(req.getContent().trim()).setStatus(CommentStatusEnum.NORMAL)
            .setDeleted(DeleteStatusEnum.NOT_DELETED).setRootId(NONE).setParentId(NONE).setReplyToUserId(NONE);

        Long parentId = req.getParentId();
        if (parentId != null && parentId > NONE) {
            CommentDO parent = commentDAO.getByIdAndNotDeleted(parentId);
            if (parent == null || !parent.getArticleId().equals(articleId) || !isVisible(parent)) {
                throw ResultCode.COMMENT_NOT_EXISTS.toException();
            }
            boolean parentIsRoot = parent.getRootId() == null || parent.getRootId() == NONE;
            comment.setRootId(parentIsRoot ? parent.getId() : parent.getRootId()).setParentId(parent.getId())
                .setReplyToUserId(parentIsRoot ? NONE : parent.getUserId());
        }

        commentDAO.save(comment);
        articlePort.increaseCommentCount(articleId, 1);
        return comment.getId();
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public void deleteComment(Long userId, Long articleId, Long commentId) {
        CommentDO comment = commentDAO.getByIdAndNotDeleted(commentId);
        if (comment == null || !comment.getArticleId().equals(articleId)) {
            throw ResultCode.COMMENT_NOT_EXISTS.toException();
        }
        if (!comment.getUserId().equals(userId)) {
            throw ResultCode.COMMENT_NO_PERMISSION.toException();
        }

        int deleted = commentDAO.delete(List.of(commentId), comment.getStatus());
        if (deleted > 0 && CommentStatusEnum.NORMAL.equals(comment.getStatus())) {
            articlePort.decreaseCommentCount(articleId, deleted);
        }
    }

    @Override
    public PageVO<CommentAdminVO> pageForAdmin(CommentAdminPageParam req) {
        IPage<CommentDO> result = commentDAO.pageForAdmin(req, req.toPage());
        List<CommentDO> comments = result.getRecords();
        if (comments.isEmpty()) {
            return PageVO.of(result, List.of());
        }

        Map<Long, CommentUserVO> userMap =
            loadUserMap(comments.stream().map(CommentDO::getUserId).collect(Collectors.toSet()));
        Map<Long, String> titleMap =
            articlePort.getArticleTitles(comments.stream().map(CommentDO::getArticleId).collect(Collectors.toSet()));

        List<CommentAdminVO> content = comments.stream().map(comment -> {
            CommentAdminVO vo = commentConverter.toAdminVO(comment);
            vo.setUser(userMap.get(comment.getUserId()));
            vo.setArticleTitle(titleMap.get(comment.getArticleId()));
            return vo;
        }).toList();
        return PageVO.of(result, content);
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public void updateStatus(Long commentId, CommentStatusEnum status) {
        CommentDO comment = commentDAO.getByIdAndNotDeleted(commentId);
        if (comment == null) {
            throw ResultCode.COMMENT_NOT_EXISTS.toException();
        }
        CommentStatusEnum current = comment.getStatus();
        if (status.equals(current)) {
            return;
        }
        // 条件更新未命中说明已被并发改过，此时不能再调整计数，否则会重复增减
        if (!commentDAO.updateStatus(commentId, current, status)) {
            return;
        }

        if (CommentStatusEnum.HIDDEN.equals(status)) {
            articlePort.decreaseCommentCount(comment.getArticleId(), 1);
        } else {
            articlePort.increaseCommentCount(comment.getArticleId(), 1);
        }
        log.info("评论审核 commentId={} {} -> {}", commentId, current, status);
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public void batchDelete(List<Long> commentIds) {
        List<CommentDO> comments = commentDAO.listByIdsAndNotDeleted(commentIds);
        if (comments.isEmpty()) {
            return;
        }

        // 隐藏态评论不计入 comment_count，删除它们无需调整计数，故与正常态分开处理
        groupIdsByArticle(comments, CommentStatusEnum.HIDDEN).values()
            .forEach(ids -> commentDAO.delete(ids, CommentStatusEnum.HIDDEN));

        groupIdsByArticle(comments, CommentStatusEnum.NORMAL).forEach((articleId, ids) -> {
            int deleted = commentDAO.delete(ids, CommentStatusEnum.NORMAL);
            if (deleted > 0) {
                articlePort.decreaseCommentCount(articleId, deleted);
            }
        });
        log.info("批量删除评论 count={} ids={}", comments.size(), commentIds);
    }

    /**
     * 按文章分组指定状态的评论 ID，使计数更新按文章聚合成一次，避免逐条更新
     */
    private Map<Long, List<Long>> groupIdsByArticle(List<CommentDO> comments, CommentStatusEnum status) {
        return comments.stream().filter(comment -> status.equals(comment.getStatus())).collect(
            Collectors.groupingBy(CommentDO::getArticleId, Collectors.mapping(CommentDO::getId, Collectors.toList())));
    }

    private boolean isVisible(CommentDO comment) {
        return DeleteStatusEnum.NOT_DELETED.equals(comment.getDeleted())
            && CommentStatusEnum.NORMAL.equals(comment.getStatus());
    }

    /**
     * 限流仅针对普通读者。管理员是站点作者，连续回复多条评论是其正常工作方式， 受自己设的反垃圾规则约束没有意义（WordPress 对可审核评论的角色同样豁免）
     */
    private void checkRateLimit(Long userId) {
        ReqInfoContext.ReqInfo reqInfo = ReqInfoContext.getContext();
        if (reqInfo != null && reqInfo.isAdmin()) {
            return;
        }

        CommentProperties.RateLimit rateLimit = commentProperties.getRateLimit();
        // 先查间隔再查窗口：间隔不过就短路返回，避免为一个必然被拒的请求白白消耗窗口配额
        ensureAcquired(rateLimiter.tryAcquire(RedisKeyConstants.getCommentIntervalKey(userId), INTERVAL_QUOTA,
            rateLimit.getMinInterval()));
        ensureAcquired(rateLimiter.tryAcquire(RedisKeyConstants.getCommentHourlyKey(userId),
            rateLimit.getMaxPerWindow(), rateLimit.getWindow()));
    }

    private void ensureAcquired(RateLimiter.Result result) {
        if (result.allowed()) {
            return;
        }
        throw new BusinessException(ResultCode.COMMENT_TOO_FREQUENT.getCode(),
            "评论过于频繁，请 " + result.retryAfterSeconds() + " 秒后再试");
    }

    private Map<Long, CommentUserVO> loadUsers(List<CommentDO> roots, Map<Long, List<CommentDO>> repliesByRoot) {
        Set<Long> userIds = new HashSet<>();
        roots.stream().filter(this::isVisible).map(CommentDO::getUserId).forEach(userIds::add);
        repliesByRoot.values().stream().flatMap(List::stream).forEach(reply -> {
            userIds.add(reply.getUserId());
            if (reply.getReplyToUserId() != null && reply.getReplyToUserId() > NONE) {
                userIds.add(reply.getReplyToUserId());
            }
        });
        return loadUserMap(userIds);
    }

    private Map<Long, CommentUserVO> loadUserMap(Set<Long> userIds) {
        if (userIds.isEmpty()) {
            return Map.of();
        }
        return userPort.getUserBatchByIds(List.copyOf(userIds)).stream()
            .collect(Collectors.toMap(UserVO::getUserId, commentConverter::toCommentUser, (a, b) -> a));
    }

    private CommentVO buildRootVO(CommentDO root, List<CommentDO> replies, Map<Long, CommentUserVO> userMap) {
        CommentVO vo = commentConverter.toVO(root);
        boolean visible = isVisible(root);
        vo.setRemoved(!visible);
        if (visible) {
            vo.setUser(userMap.get(root.getUserId()));
        } else {
            vo.setContent(null);
        }
        vo.setReplies(replies.stream().map(reply -> buildReplyVO(reply, userMap)).toList());
        vo.setReplyCount(replies.size());
        return vo;
    }

    private CommentReplyVO buildReplyVO(CommentDO reply, Map<Long, CommentUserVO> userMap) {
        CommentReplyVO vo = commentConverter.toReplyVO(reply);
        vo.setUser(userMap.get(reply.getUserId()));
        if (reply.getReplyToUserId() != null && reply.getReplyToUserId() > NONE) {
            vo.setReplyToUser(userMap.get(reply.getReplyToUserId()));
        }
        return vo;
    }
}
