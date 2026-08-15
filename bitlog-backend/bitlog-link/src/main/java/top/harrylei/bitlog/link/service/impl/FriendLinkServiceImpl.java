package top.harrylei.bitlog.link.service.impl;

import com.baomidou.mybatisplus.core.metadata.IPage;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.context.ApplicationEventPublisher;
import org.springframework.stereotype.Service;
import org.springframework.util.StringUtils;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.transaction.support.TransactionSynchronization;
import org.springframework.transaction.support.TransactionSynchronizationManager;
import top.harrylei.bitlog.api.enums.link.FriendLinkStatusEnum;
import top.harrylei.bitlog.api.model.link.query.FriendLinkPageParam;
import top.harrylei.bitlog.api.model.link.req.FriendLinkAuditParam;
import top.harrylei.bitlog.api.model.link.req.FriendLinkSaveParam;
import top.harrylei.bitlog.api.model.link.vo.FriendLinkAdminVO;
import top.harrylei.bitlog.api.model.link.vo.FriendLinkVO;
import top.harrylei.bitlog.api.model.link.vo.MyFriendLinkVO;
import top.harrylei.bitlog.common.constants.RedisKeyConstants;
import top.harrylei.bitlog.common.enums.ResultCode;
import top.harrylei.bitlog.common.exception.BusinessException;
import top.harrylei.bitlog.api.model.user.vo.UserVO;
import top.harrylei.bitlog.common.model.PageVO;
import top.harrylei.bitlog.common.context.ReqInfoContext;
import top.harrylei.bitlog.common.util.RateLimiter;
import top.harrylei.bitlog.file.service.FileService;
import top.harrylei.bitlog.file.util.FileUrlHelper;
import top.harrylei.bitlog.link.event.FriendLinkApprovedEvent;
import top.harrylei.bitlog.user.port.UserPort;
import top.harrylei.bitlog.link.config.FriendLinkProperties;
import top.harrylei.bitlog.link.converter.FriendLinkConverter;
import top.harrylei.bitlog.link.repository.dao.FriendLinkDAO;
import top.harrylei.bitlog.link.repository.entity.FriendLinkDO;
import top.harrylei.bitlog.link.service.FriendLinkService;
import top.harrylei.bitlog.link.support.SiteUrlNormalizer;

import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.function.Function;
import java.util.stream.Collectors;

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
    private final UserPort userPort;
    private final FileUrlHelper fileUrlHelper;
    private final FileService fileService;
    private final ApplicationEventPublisher eventPublisher;

    @Override
    public List<FriendLinkVO> listApproved() {
        List<FriendLinkVO> list = friendLinkConverter.toVOList(friendLinkDAO.listApproved());
        list.forEach(vo -> vo.setAvatar(resolveAvatar(vo.getAvatar())));
        return list;
    }

    @Override
    public MyFriendLinkVO getMine(Long userId) {
        MyFriendLinkVO vo = friendLinkConverter.toMyVO(friendLinkDAO.getByUserId(userId));
        return vo == null ? null : vo.setAvatar(resolveAvatar(vo.getAvatar()));
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public Long applyMine(Long userId, FriendLinkSaveParam param) {
        checkRateLimit(userId);

        if (friendLinkDAO.getByUserId(userId) != null) {
            ResultCode.LINK_ALREADY_APPLIED.throwException();
        }

        String url = normalizeOrThrow(param.getUrl());
        checkUrlAvailable(url, null);

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
            throw ResultCode.LINK_NOT_EXISTS.toException();
        }

        String url = normalizeOrThrow(param.getUrl());
        boolean urlChanged = !Objects.equals(url, friendLink.getUrl());
        if (urlChanged) {
            checkUrlAvailable(url, friendLink.getId());
        }

        String oldAvatar = friendLink.getAvatar();
        friendLinkConverter.applyToEntity(param, friendLink);
        applyOptionalFields(friendLink, param);
        friendLink.setUrl(url);

        friendLinkDAO.updateContent(friendLink);

        // 换了站点地址等于换了一个待审对象，已通过的必须重新过审，否则用一个正经站点
        // 过审后改成任意地址就绕开了审核。被拒的则是改完即重新提交，不然永远出不了拒绝态。
        if (urlChanged || friendLink.getStatus() == FriendLinkStatusEnum.REJECTED) {
            friendLinkDAO.updateStatus(friendLink.getId(), FriendLinkStatusEnum.PENDING, null);
            friendLink.setStatus(FriendLinkStatusEnum.PENDING);
        }
        onAvatarChanged(friendLink, oldAvatar);

        log.info("修改友链 userId={} linkId={} urlChanged={} status={}", userId, friendLink.getId(), urlChanged,
            friendLink.getStatus());
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public void deleteMine(Long userId) {
        checkRateLimit(userId);

        FriendLinkDO friendLink = friendLinkDAO.getByUserId(userId);
        if (friendLink == null) {
            throw ResultCode.LINK_NOT_EXISTS.toException();
        }

        friendLinkDAO.removeById(friendLink.getId());
        deleteObjectAfterCommit(friendLink.getAvatar());
        log.info("删除友链 userId={} linkId={}", userId, friendLink.getId());
    }

    @Override
    public PageVO<FriendLinkAdminVO> pageForAdmin(FriendLinkPageParam param) {
        IPage<FriendLinkDO> result = friendLinkDAO.pageForAdmin(param, param.toPage());
        List<FriendLinkDO> links = result.getRecords();
        if (links.isEmpty()) {
            return PageVO.of(result, List.of());
        }

        Map<Long, UserVO> applicants = loadApplicants(links);
        List<FriendLinkAdminVO> content = links.stream().map(link -> {
            FriendLinkAdminVO vo = friendLinkConverter.toAdminVO(link);
            vo.setApplicant(link.getUserId() == null ? null : applicants.get(link.getUserId()));
            vo.setAvatar(resolveAvatar(vo.getAvatar()));
            return vo;
        }).toList();
        return PageVO.of(result, content);
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public Long saveByAdmin(FriendLinkSaveParam param) {
        String url = normalizeOrThrow(param.getUrl());
        checkUrlAvailable(url, null);

        FriendLinkDO friendLink = new FriendLinkDO();
        friendLinkConverter.applyToEntity(param, friendLink);
        applyOptionalFields(friendLink, param);
        // userId 留空：站长录入的友链没有归属账号，无人可自助管理
        friendLink.setUrl(url).setStatus(FriendLinkStatusEnum.APPROVED);
        friendLinkDAO.save(friendLink);

        publishApproved(friendLink.getId(), friendLink.getAvatar());
        log.info("站长录入友链 linkId={} url={}", friendLink.getId(), url);
        return friendLink.getId();
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public void updateByAdmin(Long id, FriendLinkSaveParam param) {
        FriendLinkDO friendLink = getExisting(id);

        String url = normalizeOrThrow(param.getUrl());
        if (!Objects.equals(url, friendLink.getUrl())) {
            checkUrlAvailable(url, id);
        }

        String oldAvatar = friendLink.getAvatar();
        friendLinkConverter.applyToEntity(param, friendLink);
        applyOptionalFields(friendLink, param);
        friendLink.setUrl(url);
        // 只改内容：站长自己改的东西不必退回自己审，状态列也就不该被这次写入碰到
        friendLinkDAO.updateContent(friendLink);
        onAvatarChanged(friendLink, oldAvatar);

        log.info("站长修改友链 linkId={}", id);
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public void audit(Long id, FriendLinkAuditParam param) {
        FriendLinkDO friendLink = getExisting(id);

        FriendLinkStatusEnum target = param.getStatus();
        if (target != FriendLinkStatusEnum.APPROVED && target != FriendLinkStatusEnum.REJECTED) {
            ResultCode.LINK_STATUS_ILLEGAL.throwException("审核结果只能是通过或拒绝");
        }
        // 只挡重复通过。重复拒绝要放行：理由是给申请人看的，写错了得能改，
        // 拦住的话只能先通过再拒绝，而中间那一下会把站点真的挂上公开页。
        if (target == FriendLinkStatusEnum.APPROVED && friendLink.getStatus() == FriendLinkStatusEnum.APPROVED) {
            ResultCode.LINK_STATUS_ILLEGAL.throwException("该友链已通过审核");
        }

        // 通过时清空拒绝理由，否则上一次拒绝的说明会残留，申请人再被拒时看到的是旧文案
        String reason = target == FriendLinkStatusEnum.REJECTED ? trimToNull(param.getRejectReason()) : null;
        if (!friendLinkDAO.updateStatus(id, target, reason)) {
            ResultCode.LINK_NOT_EXISTS.throwException();
        }

        if (target == FriendLinkStatusEnum.APPROVED) {
            publishApproved(id, friendLink.getAvatar());
        }
        log.info("审核友链 linkId={} {} -> {}", id, friendLink.getStatus(), target);
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public void deleteByAdmin(Long id) {
        FriendLinkDO friendLink = getExisting(id);
        friendLinkDAO.removeById(id);
        deleteObjectAfterCommit(friendLink.getAvatar());
        log.info("站长删除友链 linkId={}", id);
    }

    /**
     * 头像列同时存放两种值：尚未转存的外链，与转存后的对象存储 key。 外链直接用，key 才需要拼公开前缀，否则会拼成 https://存储域名/https://对方域名/...
     */
    private String resolveAvatar(String avatar) {
        return SiteUrlNormalizer.isValid(avatar) ? avatar : fileUrlHelper.buildUrl(avatar);
    }

    /**
     * 已是自有存储的 key 就不必再转存一次
     */
    private void publishApproved(Long linkId, String avatar) {
        if (!SiteUrlNormalizer.isValid(avatar)) {
            return;
        }
        ReqInfoContext.ReqInfo reqInfo = ReqInfoContext.getContext();
        if (reqInfo == null || reqInfo.getUserId() == null) {
            // 拿不到操作人就拼不出存储路径，保留外链即可，不值得为此让审核失败
            log.warn("无操作人上下文，跳过友链头像转存 linkId={}", linkId);
            return;
        }
        eventPublisher.publishEvent(new FriendLinkApprovedEvent(linkId, avatar, reqInfo.getUserId()));
    }

    private FriendLinkDO getExisting(Long id) {
        FriendLinkDO friendLink = friendLinkDAO.getById(id);
        if (friendLink == null) {
            throw ResultCode.LINK_NOT_EXISTS.toException();
        }
        return friendLink;
    }

    /**
     * 站点地址是否可用，selfId 为空表示新建
     */
    private void checkUrlAvailable(String url, Long selfId) {
        FriendLinkDO occupied = friendLinkDAO.getByUrl(url);
        if (occupied != null && !Objects.equals(occupied.getId(), selfId)) {
            ResultCode.LINK_URL_TAKEN.throwException();
        }
    }

    /**
     * 头像换掉后的收尾：新外链交给转存，旧的自有对象顺手回收
     * <p>
     * 只有仍在展示中的友链才转存。退回待审的等过审那一刻再转——未过审就转存等于让刷申请的人 往对象存储里灌垃圾，审核是天然的闸门。
     * </p>
     */
    private void onAvatarChanged(FriendLinkDO friendLink, String oldAvatar) {
        if (Objects.equals(oldAvatar, friendLink.getAvatar())) {
            return;
        }
        if (friendLink.getStatus() == FriendLinkStatusEnum.APPROVED) {
            publishApproved(friendLink.getId(), friendLink.getAvatar());
        }
        deleteObjectAfterCommit(oldAvatar);
    }

    /**
     * 提交后回收自有存储里的孤儿对象
     * <p>
     * 外链不是本站的东西，跳过。删除放到提交之后：事务一旦回滚，记录还在而对象已经没了。
     * </p>
     */
    private void deleteObjectAfterCommit(String avatar) {
        if (!StringUtils.hasText(avatar) || SiteUrlNormalizer.isValid(avatar)) {
            return;
        }
        TransactionSynchronizationManager.registerSynchronization(new TransactionSynchronization() {
            @Override
            public void afterCommit() {
                fileService.delete(avatar);
            }
        });
    }

    /**
     * 批量取申请人，跨域走 UserPort，不直接注入用户域的服务
     */
    private Map<Long, UserVO> loadApplicants(List<FriendLinkDO> links) {
        List<Long> userIds = links.stream().map(FriendLinkDO::getUserId).filter(Objects::nonNull).distinct().toList();
        if (userIds.isEmpty()) {
            return Map.of();
        }
        return userPort.getUserBatchByIds(userIds).stream()
            .collect(Collectors.toMap(UserVO::getUserId, Function.identity()));
    }

    /**
     * 前端的 pattern 只是体验，绕过它直接打接口是常态，协议白名单必须在这里再判一次
     */
    private String normalizeOrThrow(String url) {
        String normalized = SiteUrlNormalizer.normalize(url);
        if (normalized == null) {
            throw ResultCode.INVALID_PARAMETER.toException("站点地址不是有效的 http/https 地址");
        }
        return normalized;
    }

    /**
     * 选填字段统一：空白归一成 null，别在库里存空串
     */
    private void applyOptionalFields(FriendLinkDO friendLink, FriendLinkSaveParam param) {
        friendLink.setAvatar(normalizeAvatar(param.getAvatar())).setDescription(trimToNull(param.getDescription()))
            .setApplyMessage(trimToNull(param.getApplyMessage()));
    }

    private String trimToNull(String value) {
        return StringUtils.hasText(value) ? value.trim() : null;
    }

    /**
     * 头像选填，非法地址按未填处理——为一张图片挡下整次申请不值得，前端本就会退回站名首字
     * <p>
     * 已转存的头像出站时被拼成完整 URL，客户端回填表单后原样送回来。这里必须还原成 key： 直接存下带存储域名的绝对地址，公开地址一变这些行就全部破图，全站其余模块存的都是 key。
     * </p>
     */
    private String normalizeAvatar(String avatar) {
        if (!StringUtils.hasText(avatar)) {
            return null;
        }
        String trimmed = avatar.trim();
        String key = fileUrlHelper.extractKey(trimmed);
        if (!key.equals(trimmed)) {
            return key;
        }
        return SiteUrlNormalizer.isValid(trimmed) ? trimmed : null;
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
