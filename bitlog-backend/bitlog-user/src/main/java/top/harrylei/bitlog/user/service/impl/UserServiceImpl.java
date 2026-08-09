package top.harrylei.bitlog.user.service.impl;

import com.baomidou.mybatisplus.core.metadata.IPage;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.context.ApplicationEventPublisher;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.transaction.support.TransactionSynchronization;
import org.springframework.transaction.support.TransactionSynchronizationManager;
import org.springframework.util.StringUtils;
import top.harrylei.bitlog.api.enums.user.UserRoleEnum;
import top.harrylei.bitlog.api.enums.user.UserStatusEnum;
import top.harrylei.bitlog.api.model.user.UserRules;
import top.harrylei.bitlog.api.model.user.dto.UserDetailDTO;
import top.harrylei.bitlog.api.model.user.dto.UserStatsDTO;
import top.harrylei.bitlog.api.model.user.query.UserPageParam;
import top.harrylei.bitlog.api.model.user.req.UserUpdateParam;
import top.harrylei.bitlog.api.model.user.vo.UserDetailVO;
import top.harrylei.bitlog.api.model.user.vo.UserListVO;
import top.harrylei.bitlog.api.model.user.vo.UserStatsVO;
import top.harrylei.bitlog.common.context.ReqInfoContext;
import top.harrylei.bitlog.common.enums.DeleteStatusEnum;
import top.harrylei.bitlog.common.enums.ResultCode;
import top.harrylei.bitlog.common.model.PageVO;
import top.harrylei.bitlog.common.util.FileUrlHelper;
import top.harrylei.bitlog.file.model.UploadScene;
import top.harrylei.bitlog.file.service.FileService;
import top.harrylei.bitlog.user.converter.UserConverter;
import top.harrylei.bitlog.user.event.UserDeactivatedEvent;
import top.harrylei.bitlog.user.event.UserDisabledEvent;
import top.harrylei.bitlog.user.repository.dao.UserDAO;
import top.harrylei.bitlog.user.repository.dao.UserInfoDAO;
import top.harrylei.bitlog.user.repository.entity.UserDO;
import top.harrylei.bitlog.user.repository.entity.UserInfoDO;
import top.harrylei.bitlog.user.service.UserService;

import java.util.List;
import java.util.UUID;

/**
 * 用户业务服务实现
 *
 * @author Harry
 * @since 2026-03-28
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class UserServiceImpl implements UserService {

    /** RFC 2606 保留域，永不可能与真实邮箱冲突 */
    private static final String DEACTIVATED_EMAIL_DOMAIN = "@bitlog.invalid";

    private static final int TOMBSTONE_NAME_RETRY = 3;

    /** 前缀 4 位 + 后缀 8 位 = 12，须留在 user_account.username 的 varchar(16) 之内 */
    private static final int TOMBSTONE_SUFFIX_LENGTH = 8;

    private final UserDAO userDAO;
    private final UserInfoDAO userInfoDAO;
    private final UserConverter userConverter;
    private final FileUrlHelper fileUrlHelper;
    private final FileService fileService;
    private final ApplicationEventPublisher eventPublisher;

    @Override
    public UserDetailVO getUserDetail(Long userId) {
        UserInfoDO userInfo = userInfoDAO.getByUserId(userId);
        UserDO user = userInfo == null ? null : userDAO.getById(userInfo.getUserId());
        return buildUserDetail(userInfo, user);
    }

    @Override
    public UserDetailVO getUserDetailIncludingDeactivated(Long userId) {
        UserInfoDO userInfo = userInfoDAO.getByUserIdIncludingDeleted(userId);
        UserDO user = userInfo == null ? null : userDAO.getByIdIncludingDeleted(userInfo.getUserId());
        return buildUserDetail(userInfo, user);
    }

    private UserDetailVO buildUserDetail(UserInfoDO userInfo, UserDO user) {
        if (userInfo == null || user == null) {
            throw ResultCode.USER_NOT_EXISTS.toException();
        }
        UserDetailVO vo = userConverter.toDetailVO(userInfo, user);
        vo.setAvatar(fileUrlHelper.buildUrl(vo.getAvatar()));
        vo.setHasPassword(StringUtils.hasText(user.getPassword()));
        return vo;
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public void updateUserInfo(Long userId, UserUpdateParam req) {
        UserInfoDO userInfo = userInfoDAO.getByUserId(userId);
        if (userInfo == null) {
            ResultCode.USER_NOT_EXISTS.throwException();
        }
        String username = req.getUsername().trim();
        if (UserRules.isReserved(username) || userDAO.isUsernameTakenByOthers(username, userId)) {
            ResultCode.USER_ALREADY_EXISTS.throwException(username);
        }

        userDAO.updateUsername(userId, username);
        userInfoDAO.updateInfo(userId, req.getProfile(), req.getPosition(), req.getCompany());
        log.info("更新用户基本信息 userId={}", userId);
    }

    @Override
    public void updateAvatar(Long userId, String avatar) {
        UserInfoDO userInfo = userInfoDAO.getByUserId(userId);
        if (userInfo == null) {
            throw ResultCode.USER_NOT_EXISTS.toException();
        }
        String key = fileUrlHelper.extractKey(avatar);
        String ownerPrefix = UploadScene.AVATAR.getCode() + "/" + userId + "/";
        if (!key.startsWith(ownerPrefix)) {
            ResultCode.INVALID_PARAMETER.throwException("无效的头像地址");
        }
        String oldAvatarUrl = userInfo.getAvatar();
        userInfoDAO.updateAvatar(userId, key);
        log.info("更新用户头像 userId={}", userId);
        if (StringUtils.hasText(oldAvatarUrl)) {
            String oldKey = fileUrlHelper.extractKey(oldAvatarUrl);
            if (StringUtils.hasText(oldKey) && !oldKey.contains("://")) {
                fileService.delete(oldKey);
            }
        }
    }

    @Override
    public void updateUserStatusBatch(List<Long> userIds, UserStatusEnum status) {
        checkNotSelf(userIds);
        checkNotAdmin(userIds);
        userDAO.updateStatusBatch(userIds, status);
        // 启用不必发事件：被封期间会话早已清空
        if (UserStatusEnum.DISABLED.equals(status)) {
            eventPublisher.publishEvent(new UserDisabledEvent(userIds));
        }
        log.info("批量修改用户状态 userIds={} status={}", userIds, status);
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public void deactivateUserBatch(List<Long> userIds) {
        // 管理员不可注销：文章只有管理员能写，注销会让 article.user_id 悬空
        checkNotAdmin(userIds);
        userIds.forEach(this::tombstone);
    }

    /**
     * 墓碑化本体：覆写唯一列腾空 uk_username/uk_email，匿名化资料并解绑第三方， 账号行以 deleted=1 保留供评论继续引用。已注销账号重复调用直接返回。
     */
    private void tombstone(Long userId) {
        UserDO user = userDAO.getByIdIncludingDeleted(userId);
        if (user == null || DeleteStatusEnum.DELETED.equals(user.getDeleted())) {
            return;
        }

        // 头像 Key 须在 anonymize 清空该字段之前取出
        UserInfoDO userInfo = userInfoDAO.getByUserIdIncludingDeleted(userId);

        String tombstone = generateTombstoneName();
        userDAO.deactivate(userId, tombstone, tombstone + DEACTIVATED_EMAIL_DOMAIN);
        userInfoDAO.anonymize(userId);
        // 解绑第三方与撤销会话属认证侧职责，同步监听器在本事务内完成
        eventPublisher.publishEvent(new UserDeactivatedEvent(userId));

        // 内容图片不在此处理：ImageCleanupTask 按引用扫描回收孤儿，比按 user_id 删更安全
        if (userInfo != null && StringUtils.hasText(userInfo.getAvatar())) {
            deleteObjectAfterCommit(fileUrlHelper.extractKey(userInfo.getAvatar()));
        }

        log.info("账号注销 userId={}", userId);
    }

    @Override
    public UserStatsVO getUserStats() {
        UserStatsDTO dto = userDAO.countStats();
        UserStatsVO vo = new UserStatsVO();
        vo.setTotal(dto.getTotal());
        vo.setEnabled(dto.getEnabled());
        vo.setDisabled(dto.getDisabled());
        vo.setDeactivated(dto.getDeactivated());
        return vo;
    }

    private void checkNotSelf(List<Long> userIds) {
        Long currentUserId = ReqInfoContext.getContext().getUserId();
        if (userIds.contains(currentUserId)) {
            ResultCode.OPERATION_NOT_ALLOWED.throwException("不能对自己的账号执行此操作");
        }
    }

    private void checkNotAdmin(List<Long> userIds) {
        // TODO: 后续支持分级管理员后，改为只拦同级或更高权限账号
        // TODO: 检查是否为最后一个管理员，防止系统失去管理员
        List<UserDO> users = userDAO.listByUserIds(userIds);
        boolean hasAdmin = users.stream().anyMatch(user -> UserRoleEnum.ADMIN.equals(user.getUserRole()));
        if (hasAdmin) {
            ResultCode.OPERATION_NOT_ALLOWED.throwException("不能操作管理员账号");
        }
    }

    /** 用随机串而非 userId，避免把内部主键印在评论区供反查 */
    private String generateTombstoneName() {
        for (int i = 0; i < TOMBSTONE_NAME_RETRY; i++) {
            String candidate = UserRules.DEACTIVATED_PREFIX
                + UUID.randomUUID().toString().replace("-", "").substring(0, TOMBSTONE_SUFFIX_LENGTH);
            if (!userDAO.isUsernameTaken(candidate)) {
                return candidate;
            }
        }
        log.error("墓碑用户名生成失败，连续 {} 次随机候选均已被占用", TOMBSTONE_NAME_RETRY);
        throw ResultCode.INTERNAL_ERROR.toException();
    }

    /** 对象存储删除是外部 IO，事务回滚时文件不应已被删除 */
    private void deleteObjectAfterCommit(String key) {
        if (!StringUtils.hasText(key)) {
            return;
        }
        TransactionSynchronizationManager.registerSynchronization(new TransactionSynchronization() {
            @Override
            public void afterCommit() {
                fileService.delete(key);
            }
        });
    }

    @Override
    public PageVO<UserListVO> pageQuery(UserPageParam query) {
        IPage<UserDetailDTO> resultPage = userDAO.pageUsers(query, query.toPage());

        List<UserListVO> voList = resultPage.getRecords().stream().map(dto -> {
            UserListVO vo = userConverter.toListVO(dto);
            vo.setAvatar(fileUrlHelper.buildUrl(vo.getAvatar()));
            return vo;
        }).toList();

        return PageVO.of(resultPage, voList);
    }
}
