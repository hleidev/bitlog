package top.harrylei.bitlog.link.service.impl;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.util.StringUtils;
import org.springframework.transaction.annotation.Transactional;
import top.harrylei.bitlog.api.enums.link.FriendLinkStatusEnum;
import top.harrylei.bitlog.api.model.link.req.FriendLinkSaveParam;
import top.harrylei.bitlog.api.model.link.vo.FriendLinkVO;
import top.harrylei.bitlog.api.model.link.vo.MyFriendLinkVO;
import top.harrylei.bitlog.common.constants.RedisKeyConstants;
import top.harrylei.bitlog.common.enums.ResultCode;
import top.harrylei.bitlog.common.exception.BusinessException;
import top.harrylei.bitlog.common.util.RateLimiter;
import top.harrylei.bitlog.link.config.FriendLinkProperties;
import top.harrylei.bitlog.link.converter.FriendLinkConverter;
import top.harrylei.bitlog.link.repository.dao.FriendLinkDAO;
import top.harrylei.bitlog.link.repository.entity.FriendLinkDO;
import top.harrylei.bitlog.link.service.FriendLinkService;
import top.harrylei.bitlog.link.support.SiteUrlNormalizer;

import java.util.List;
import java.util.Objects;

/**
 * 友链服务实现
 *
 * @author Harry
 * @since 2026-08-14
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class FriendLinkServiceImpl implements FriendLinkService {

    private final FriendLinkDAO friendLinkDAO;
    private final FriendLinkConverter friendLinkConverter;
    private final FriendLinkProperties friendLinkProperties;
    private final RateLimiter rateLimiter;

    @Override
    public List<FriendLinkVO> listApproved() {
        return friendLinkConverter.toVOList(friendLinkDAO.listApproved());
    }

    @Override
    public MyFriendLinkVO getMine(Long userId) {
        return friendLinkConverter.toMyVO(friendLinkDAO.getByUserId(userId));
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public Long applyMine(Long userId, FriendLinkSaveParam param) {
        checkRateLimit(userId);

        if (friendLinkDAO.getByUserId(userId) != null) {
            throw new BusinessException(ResultCode.LINK_ALREADY_APPLIED.getCode(), ResultCode.LINK_ALREADY_APPLIED.getMessage());
        }

        String url = normalizeOrThrow(param.getUrl());
        if (friendLinkDAO.getByUrl(url) != null) {
            throw new BusinessException(ResultCode.LINK_URL_TAKEN.getCode(), ResultCode.LINK_URL_TAKEN.getMessage());
        }

        FriendLinkDO friendLink = new FriendLinkDO();
        friendLinkConverter.applyToEntity(param, friendLink);
        applyOptionalFields(friendLink, param);
        friendLink.setUrl(url).setUserId(userId).setStatus(FriendLinkStatusEnum.PENDING);
        friendLinkDAO.save(friendLink);

        log.info("提交友链申请 userId={} linkId={} url={}", userId, friendLink.getId(), url);
        return friendLink.getId();
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public void updateMine(Long userId, FriendLinkSaveParam param) {
        checkRateLimit(userId);

        FriendLinkDO friendLink = friendLinkDAO.getByUserId(userId);
        if (friendLink == null) {
            throw new BusinessException(ResultCode.LINK_NOT_EXISTS.getCode(), ResultCode.LINK_NOT_EXISTS.getMessage());
        }

        String url = normalizeOrThrow(param.getUrl());
        boolean urlChanged = !Objects.equals(url, friendLink.getUrl());
        if (urlChanged) {
            FriendLinkDO occupied = friendLinkDAO.getByUrl(url);
            if (occupied != null && !Objects.equals(occupied.getId(), friendLink.getId())) {
                throw new BusinessException(ResultCode.LINK_URL_TAKEN.getCode(), ResultCode.LINK_URL_TAKEN.getMessage());
            }
        }

        friendLinkConverter.applyToEntity(param, friendLink);
        applyOptionalFields(friendLink, param);
        friendLink.setUrl(url);

        // 换了站点地址等于换了一个待审对象，已通过的必须重新过审，否则用一个正经站点
        // 过审后改成任意地址就绕开了审核。被拒的则是改完即重新提交，不然永远出不了拒绝态。
        if (urlChanged || friendLink.getStatus() == FriendLinkStatusEnum.REJECTED) {
            friendLink.setStatus(FriendLinkStatusEnum.PENDING).setRejectReason(null);
        }
        friendLinkDAO.updateSelfService(friendLink);

        log.info("修改友链 userId={} linkId={} urlChanged={} status={}", userId, friendLink.getId(), urlChanged,
            friendLink.getStatus());
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public void deleteMine(Long userId) {
        checkRateLimit(userId);

        FriendLinkDO friendLink = friendLinkDAO.getByUserId(userId);
        if (friendLink == null) {
            throw new BusinessException(ResultCode.LINK_NOT_EXISTS.getCode(), ResultCode.LINK_NOT_EXISTS.getMessage());
        }

        friendLinkDAO.removeById(friendLink.getId());
        log.info("删除友链 userId={} linkId={}", userId, friendLink.getId());
    }

    /**
     * 前端的 pattern 只是体验，绕过它直接打接口是常态，协议白名单必须在这里再判一次
     */
    private String normalizeOrThrow(String url) {
        String normalized = SiteUrlNormalizer.normalize(url);
        if (normalized == null) {
            throw new BusinessException(ResultCode.INVALID_PARAMETER.getCode(), "站点地址不是有效的 http/https 地址");
        }
        return normalized;
    }

    /**
     * 选填字段统一：空白归一成 null，别在库里存空串
     */
    private void applyOptionalFields(FriendLinkDO friendLink, FriendLinkSaveParam param) {
        friendLink.setAvatar(normalizeAvatar(param.getAvatar()))
            .setDescription(trimToNull(param.getDescription()))
            .setApplyMessage(trimToNull(param.getApplyMessage()));
    }

    private String trimToNull(String value) {
        return StringUtils.hasText(value) ? value.trim() : null;
    }

    /**
     * 头像选填，非法地址按未填处理——为一张图片挡下整次申请不值得，前端本就会退回站名首字
     */
    private String normalizeAvatar(String avatar) {
        return SiteUrlNormalizer.isValid(avatar) ? avatar.trim() : null;
    }

    private void checkRateLimit(Long userId) {
        FriendLinkProperties.RateLimit rateLimit = friendLinkProperties.getRateLimit();
        RateLimiter.Result result = rateLimiter.tryAcquire(RedisKeyConstants.getLinkWriteKey(userId),
            rateLimit.getMaxPerWindow(), rateLimit.getWindow());
        if (!result.allowed()) {
            throw new BusinessException(ResultCode.LINK_TOO_FREQUENT.getCode(),
                "操作过于频繁，请 " + result.retryAfterSeconds() + " 秒后再试");
        }
    }
}
