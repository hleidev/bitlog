package top.harrylei.bitlog.comment.service.impl;

import com.baomidou.mybatisplus.core.metadata.IPage;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import top.harrylei.bitlog.api.enums.comment.CommentStatusEnum;
import top.harrylei.bitlog.api.model.comment.query.CommentAdminPageParam;
import top.harrylei.bitlog.api.model.comment.req.CommentSaveParam;
import top.harrylei.bitlog.api.model.comment.vo.CommentAdminVO;
import top.harrylei.bitlog.api.model.comment.vo.CommentReplyVO;
import top.harrylei.bitlog.api.model.comment.vo.CommentUserVO;
import top.harrylei.bitlog.api.model.comment.vo.CommentVO;
import top.harrylei.bitlog.api.model.user.vo.UserVO;
import top.harrylei.bitlog.article.service.ArticleService;
import top.harrylei.bitlog.article.service.ArticleStatisticsService;
import top.harrylei.bitlog.comment.converter.CommentConverter;
import top.harrylei.bitlog.comment.repository.dao.CommentDAO;
import top.harrylei.bitlog.comment.repository.entity.CommentDO;
import top.harrylei.bitlog.comment.service.CommentService;
import top.harrylei.bitlog.common.constans.RedisKeyConstants;
import top.harrylei.bitlog.common.enums.DeleteStatusEnum;
import top.harrylei.bitlog.common.enums.ResultCode;
import top.harrylei.bitlog.common.model.BasePage;
import top.harrylei.bitlog.common.model.PageVO;
import top.harrylei.bitlog.common.util.RateLimiter;
import top.harrylei.bitlog.user.service.UserService;

import java.time.Duration;
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
@Service
@RequiredArgsConstructor
public class CommentServiceImpl implements CommentService {

    /**
     * root_id / parent_id / reply_to_user_id 的空值约定
     */
    private static final long NONE = 0L;

    private static final Duration MIN_INTERVAL = Duration.ofSeconds(15);
    private static final Duration HOURLY_WINDOW = Duration.ofHours(1);
    private static final int HOURLY_LIMIT = 10;

    private final CommentDAO commentDAO;
    private final CommentConverter commentConverter;
    private final ArticleService articleService;
    private final ArticleStatisticsService articleStatisticsService;
    private final UserService userService;
    private final RateLimiter rateLimiter;

    @Override
    public PageVO<CommentVO> pageComments(Long articleId, BasePage page) {
        if (!articleService.isPublished(articleId)) {
            throw ResultCode.ARTICLE_NOT_PUBLISHED.toException();
        }

        IPage<CommentDO> rootPage =
            commentDAO.pageRootComments(articleId, new Page<>(page.getPageNum(), page.getPageSize()));
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
        if (!articleService.isPublished(articleId)) {
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
            boolean parentIsRoot = parent.getRootId() == NONE;
            comment.setRootId(parentIsRoot ? parent.getId() : parent.getRootId()).setParentId(parent.getId())
                .setReplyToUserId(parentIsRoot ? NONE : parent.getUserId());
        }

        commentDAO.save(comment);
        articleStatisticsService.incrementCommentCount(articleId);
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

        commentDAO.delete(List.of(commentId));
        if (CommentStatusEnum.NORMAL.equals(comment.getStatus())) {
            articleStatisticsService.decrementCommentCount(articleId);
        }
    }

    @Override
    public PageVO<CommentAdminVO> pageForAdmin(CommentAdminPageParam req) {
        IPage<CommentDO> result = commentDAO.pageForAdmin(req, new Page<>(req.getPageNum(), req.getPageSize()));
        List<CommentDO> comments = result.getRecords();
        if (comments.isEmpty()) {
            return PageVO.of(result, List.of());
        }

        Map<Long, CommentUserVO> userMap =
            loadUserMap(comments.stream().map(CommentDO::getUserId).collect(Collectors.toSet()));
        Map<Long, String> titleMap =
            articleService.getArticleTitles(comments.stream().map(CommentDO::getArticleId).collect(Collectors.toSet()));

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
        if (status.equals(comment.getStatus())) {
            return;
        }

        commentDAO.updateStatus(commentId, status);
        if (CommentStatusEnum.HIDDEN.equals(status)) {
            articleStatisticsService.decrementCommentCount(comment.getArticleId());
        } else {
            articleStatisticsService.incrementCommentCount(comment.getArticleId());
        }
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public void batchDelete(List<Long> commentIds) {
        List<CommentDO> comments = commentDAO.listByIdsAndNotDeleted(commentIds);
        if (comments.isEmpty()) {
            return;
        }

        commentDAO.delete(comments.stream().map(CommentDO::getId).toList());
        comments.stream().filter(comment -> CommentStatusEnum.NORMAL.equals(comment.getStatus()))
            .forEach(comment -> articleStatisticsService.decrementCommentCount(comment.getArticleId()));
    }

    private boolean isVisible(CommentDO comment) {
        return DeleteStatusEnum.NOT_DELETED.equals(comment.getDeleted())
            && CommentStatusEnum.NORMAL.equals(comment.getStatus());
    }

    private void checkRateLimit(Long userId) {
        if (!rateLimiter.tryAcquire(RedisKeyConstants.getCommentIntervalKey(userId), 1, MIN_INTERVAL)) {
            throw ResultCode.COMMENT_TOO_FREQUENT.toException();
        }
        if (!rateLimiter.tryAcquire(RedisKeyConstants.getCommentHourlyKey(userId), HOURLY_LIMIT, HOURLY_WINDOW)) {
            throw ResultCode.COMMENT_TOO_FREQUENT.toException();
        }
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
        return userService.getUserBatchByIds(List.copyOf(userIds)).stream()
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
