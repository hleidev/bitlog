package top.harrylei.community.user.service.impl;

import com.baomidou.mybatisplus.core.metadata.IPage;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import top.harrylei.community.api.enums.user.UserFollowStatusEnum;
import top.harrylei.community.api.model.user.query.UserFollowPageQuery;
import top.harrylei.community.api.model.user.vo.UserFollowVO;
import top.harrylei.community.common.enums.ResultCode;
import top.harrylei.community.common.model.PageVO;
import top.harrylei.community.user.repository.dao.UserFollowDAO;
import top.harrylei.community.user.repository.dao.UserInfoDAO;
import top.harrylei.community.user.repository.entity.UserFollowDO;
import top.harrylei.community.user.service.UserFollowService;

/**
 * 用户关注业务服务实现
 *
 * @author harry
 * @since 0.0.1
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class UserFollowServiceImpl implements UserFollowService {

    private final UserFollowDAO userFollowDAO;
    private final UserInfoDAO userInfoDAO;

    @Override
    @Transactional(rollbackFor = Exception.class)
    public void follow(Long currentUserId, Long followUserId) {
        if (currentUserId.equals(followUserId)) {
            ResultCode.OPERATION_NOT_ALLOWED.throwException("不能关注自己");
        }

        // 验证被关注用户是否存在
        if (userInfoDAO.getByUserId(followUserId) == null) {
            ResultCode.USER_NOT_EXISTS.throwException();
        }

        UserFollowDO existing = userFollowDAO.getFollowRelation(currentUserId, followUserId);
        if (existing != null) {
            // 已有记录：更新为已关注状态
            if (UserFollowStatusEnum.FOLLOWED.equals(existing.getFollowState())) {
                log.debug("用户已关注，无需重复操作 userId={} followUserId={}", currentUserId, followUserId);
                return;
            }
            userFollowDAO.updateFollowStatus(currentUserId, followUserId, UserFollowStatusEnum.FOLLOWED);
        } else {
            // 新建关注记录
            UserFollowDO followDO = new UserFollowDO()
                    .setUserId(currentUserId)
                    .setFollowUserId(followUserId)
                    .setFollowState(UserFollowStatusEnum.FOLLOWED);
            userFollowDAO.save(followDO);
        }
        log.info("用户关注 userId={} followUserId={}", currentUserId, followUserId);
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public void unfollow(Long currentUserId, Long followUserId) {
        UserFollowDO existing = userFollowDAO.getFollowRelation(currentUserId, followUserId);
        if (existing == null || UserFollowStatusEnum.UNFOLLOWED.equals(existing.getFollowState())) {
            log.debug("用户未关注，无需取消 userId={} followUserId={}", currentUserId, followUserId);
            return;
        }
        userFollowDAO.updateFollowStatus(currentUserId, followUserId, UserFollowStatusEnum.UNFOLLOWED);
        log.info("用户取消关注 userId={} followUserId={}", currentUserId, followUserId);
    }

    @Override
    public PageVO<UserFollowVO> pageFollowing(UserFollowPageQuery query) {
        IPage<UserFollowVO> page = userFollowDAO.pageFollowingList(
                query, new Page<>(query.getPageNum(), query.getPageSize()));
        return buildPageVO(page, query);
    }

    @Override
    public PageVO<UserFollowVO> pageFollowers(UserFollowPageQuery query) {
        IPage<UserFollowVO> page = userFollowDAO.pageFollowersList(
                query, new Page<>(query.getPageNum(), query.getPageSize()));
        return buildPageVO(page, query);
    }

    /**
     * 构建分页响应对象
     */
    private PageVO<UserFollowVO> buildPageVO(IPage<UserFollowVO> page, UserFollowPageQuery query) {
        PageVO<UserFollowVO> pageVO = new PageVO<>();
        pageVO.setPageNum(page.getCurrent());
        pageVO.setPageSize(page.getSize());
        pageVO.setTotalElements(page.getTotal());
        pageVO.setTotalPages(page.getPages());
        pageVO.setHasPrevious(page.getCurrent() > 1);
        pageVO.setHasNext(page.getCurrent() < page.getPages());
        pageVO.setContent(page.getRecords());
        return pageVO;
    }
}
