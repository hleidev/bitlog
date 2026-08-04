package top.harrylei.bitlog.user.service.impl;

import com.baomidou.mybatisplus.core.metadata.IPage;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.security.crypto.password.PasswordEncoder;
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
import top.harrylei.bitlog.api.model.user.req.EmailCodeParam;
import top.harrylei.bitlog.api.model.user.req.EmailUpdateParam;
import top.harrylei.bitlog.api.model.user.req.PasswordInitParam;
import top.harrylei.bitlog.api.model.user.req.PasswordUpdateParam;
import top.harrylei.bitlog.api.model.user.req.UserUpdateParam;
import top.harrylei.bitlog.api.model.user.vo.PasswordResetVO;
import top.harrylei.bitlog.api.model.user.vo.UserDetailVO;
import top.harrylei.bitlog.api.model.user.vo.UserIdentityVO;
import top.harrylei.bitlog.api.model.user.vo.UserListVO;
import top.harrylei.bitlog.api.model.user.vo.UserStatsVO;
import top.harrylei.bitlog.api.model.user.vo.UserVO;
import top.harrylei.bitlog.common.constans.RedisKeyConstants;
import top.harrylei.bitlog.common.context.ReqInfoContext;
import top.harrylei.bitlog.common.enums.DeleteStatusEnum;
import top.harrylei.bitlog.common.enums.ResultCode;
import top.harrylei.bitlog.common.model.PageVO;
import top.harrylei.bitlog.common.util.EmailUtil;
import top.harrylei.bitlog.common.util.FileUrlHelper;
import top.harrylei.bitlog.common.util.MaskUtil;
import top.harrylei.bitlog.file.model.UploadScene;
import top.harrylei.bitlog.file.service.FileService;
import top.harrylei.bitlog.user.component.VerificationCodeService;
import top.harrylei.bitlog.user.component.VerifyCodePurpose;
import top.harrylei.bitlog.user.converter.UserConverter;
import top.harrylei.bitlog.user.repository.dao.UserDAO;
import top.harrylei.bitlog.user.repository.dao.UserIdentityDAO;
import top.harrylei.bitlog.user.repository.dao.UserInfoDAO;
import top.harrylei.bitlog.user.repository.entity.UserDO;
import top.harrylei.bitlog.user.repository.entity.UserIdentityDO;
import top.harrylei.bitlog.user.repository.entity.UserInfoDO;
import top.harrylei.bitlog.user.service.UserService;
import top.harrylei.bitlog.user.util.PasswordUtil;

import java.time.Duration;
import java.util.List;
import java.util.Map;
import java.util.UUID;
import java.util.function.Function;
import java.util.stream.Collectors;

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

    /** 够走完一次 Google 授权即可，留长了等于放大重放窗口 */
    private static final Duration BIND_INTENT_TTL = Duration.ofMinutes(5);

    /** 注销后对外统一展示的用户名，真实用户名已被墓碑值覆写 */
    private static final String DEACTIVATED_USERNAME = "已注销用户";

    /** RFC 2606 保留域，永不可能与真实邮箱冲突 */
    private static final String DEACTIVATED_EMAIL_DOMAIN = "@bitlog.invalid";

    private static final int TOMBSTONE_NAME_RETRY = 3;

    /** 前缀 4 位 + 后缀 8 位 = 12，须留在 user_account.username 的 varchar(16) 之内 */
    private static final int TOMBSTONE_SUFFIX_LENGTH = 8;

    private final UserDAO userDAO;
    private final UserIdentityDAO userIdentityDAO;
    private final UserInfoDAO userInfoDAO;
    private final UserConverter userConverter;
    private final PasswordEncoder passwordEncoder;
    private final FileUrlHelper fileUrlHelper;
    private final FileService fileService;
    private final VerificationCodeService verificationCodeService;
    private final StringRedisTemplate redisTemplate;

    @Override
    public UserVO getUserById(Long userId) {
        if (userId == null) {
            return null;
        }
        UserInfoDO userInfo = userInfoDAO.getByUserIdIncludingDeleted(userId);
        if (userInfo == null) {
            return null;
        }
        return buildUserVO(userInfo, userDAO.getByIdIncludingDeleted(userInfo.getUserId()));
    }

    @Override
    public List<UserVO> getUserBatchByIds(List<Long> userIds) {
        if (userIds == null || userIds.isEmpty()) {
            return List.of();
        }
        List<UserInfoDO> userInfoList = userInfoDAO.listByUserIdsIncludingDeleted(userIds);
        if (userInfoList.isEmpty()) {
            return List.of();
        }

        List<Long> accountIds = userInfoList.stream().map(UserInfoDO::getUserId).toList();
        List<UserDO> userList = userDAO.listByUserIdsIncludingDeleted(accountIds);
        Map<Long, UserDO> userMap = userList.stream().collect(Collectors.toMap(UserDO::getId, Function.identity()));

        return userInfoList.stream().map(info -> buildUserVO(info, userMap.get(info.getUserId()))).toList();
    }

    /**
     * 展示装配。注销账号的 deleted=1，唯有展示路径需要读到该行，因此这里替换成占位身份， 鉴权与写入路径仍走过滤 deleted 的查询，天然拒绝已注销账号。
     */
    private UserVO buildUserVO(UserInfoDO userInfo, UserDO user) {
        UserVO vo = userConverter.toVO(userInfo, user);
        boolean deactivated = user != null && DeleteStatusEnum.DELETED.equals(user.getDeleted());
        vo.setDeactivated(deactivated);
        if (deactivated) {
            vo.setUsername(DEACTIVATED_USERNAME);
        }
        vo.setAvatar(fileUrlHelper.buildUrl(vo.getAvatar()));
        return vo;
    }

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
    @Transactional(rollbackFor = Exception.class)
    public void updatePassword(Long userId, PasswordUpdateParam req) {
        UserInfoDO userInfo = userInfoDAO.getByUserId(userId);
        if (userInfo == null) {
            throw ResultCode.USER_NOT_EXISTS.toException();
        }
        UserDO user = userDAO.getById(userInfo.getUserId());
        if (user == null) {
            throw ResultCode.USER_NOT_EXISTS.toException();
        }

        if (!passwordEncoder.matches(req.getOldPassword(), user.getPassword())) {
            throw ResultCode.ACCOUNT_OR_PASSWORD_ERROR.toException();
        }

        userDAO.updatePassword(user.getId(), passwordEncoder.encode(req.getNewPassword()));
        log.info("用户修改密码 userId={}", userId);
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public void initPassword(Long userId, PasswordInitParam req) {
        UserDO user = userDAO.getById(userId);
        if (user == null) {
            throw ResultCode.USER_NOT_EXISTS.toException();
        }
        // 已有密码只能走带旧密码校验的修改流程，否则会话一旦被劫持即可静默改掉密码
        if (StringUtils.hasText(user.getPassword())) {
            throw ResultCode.OPERATION_NOT_ALLOWED.toException("已设置密码，请使用修改密码");
        }

        userDAO.updatePassword(userId, passwordEncoder.encode(req.getPassword()));
        log.info("用户首次设置密码 userId={}", userId);
    }

    @Override
    public List<UserIdentityVO> listIdentities(Long userId) {
        return userIdentityDAO.listByUserId(userId).stream().map(identity -> new UserIdentityVO()
            .setProvider(identity.getProvider()).setProviderEmail(identity.getProviderEmail())).toList();
    }

    @Override
    public String createBindIntent(Long userId) {
        String token = UUID.randomUUID().toString().replace("-", "");
        redisTemplate.opsForValue().set(RedisKeyConstants.getOAuthBindIntentKey(token), String.valueOf(userId),
            BIND_INTENT_TTL);
        return token;
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public void unbindIdentity(Long userId, String provider) {
        UserDO user = userDAO.getById(userId);
        if (user == null) {
            throw ResultCode.USER_NOT_EXISTS.toException();
        }

        List<UserIdentityDO> identities = userIdentityDAO.listByUserId(userId);
        if (identities.stream().noneMatch(identity -> identity.getProvider().equals(provider))) {
            throw ResultCode.INVALID_PARAMETER.toException("未绑定该平台账号");
        }
        // 解绑后须至少保留一种登录方式：既无密码又无其他绑定时放行，账号将永久无法登录
        boolean hasOtherIdentity = identities.stream().anyMatch(identity -> !identity.getProvider().equals(provider));
        if (!StringUtils.hasText(user.getPassword()) && !hasOtherIdentity) {
            throw ResultCode.OPERATION_NOT_ALLOWED.toException("解绑后将无法登录，请先设置密码");
        }

        userIdentityDAO.unbind(userId, provider);
        log.info("解绑第三方身份 userId={} provider={}", userId, provider);
    }

    @Override
    public void sendEmailChangeCode(Long userId, EmailCodeParam req) {
        // IP 限流须先于任何邮箱存在性判断：否则「已被占用」这类提前返回的分支不受限流保护，可用于枚举探测哪些邮箱已注册
        verificationCodeService.guardSendIp();

        String newEmail = checkEmailAvailable(userId, req.getEmail());
        verificationCodeService.issueAndSend(VerifyCodePurpose.CHANGE_EMAIL, newEmail);
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public void updateEmail(Long userId, EmailUpdateParam req) {
        // 发码到提交之间有验证码有效期那么长的窗口，其间该邮箱可能被他人注册走，故此处重查一次，唯一索引为最后兜底
        String newEmail = checkEmailAvailable(userId, req.getEmail());

        verificationCodeService.verify(VerifyCodePurpose.CHANGE_EMAIL, newEmail, req.getCode());

        userDAO.updateEmail(userId, newEmail);
        log.info("用户修改邮箱 userId={} email={}", userId, MaskUtil.email(newEmail));
    }

    /**
     * 校验新邮箱可用，返回归一化后的邮箱
     */
    private String checkEmailAvailable(Long userId, String email) {
        UserDO user = userDAO.getById(userId);
        if (user == null) {
            throw ResultCode.USER_NOT_EXISTS.toException();
        }
        String newEmail = EmailUtil.normalize(email);
        if (newEmail.equals(user.getEmail())) {
            ResultCode.INVALID_PARAMETER.throwException("新邮箱与当前邮箱相同");
        }
        if (userDAO.isEmailTaken(newEmail)) {
            ResultCode.USER_ALREADY_EXISTS.throwException(newEmail);
        }
        return newEmail;
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
        userDAO.deactivate(userId, tombstone, tombstone + DEACTIVATED_EMAIL_DOMAIN,
            passwordEncoder.encode(PasswordUtil.generateRandomPassword()));
        userInfoDAO.anonymize(userId);
        userIdentityDAO.removeByUserId(userId);

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

    @Override
    @Transactional(rollbackFor = Exception.class)
    public PasswordResetVO resetPassword(Long userId) {
        UserDO user = userDAO.getById(userId);
        if (user == null) {
            ResultCode.USER_NOT_EXISTS.throwException();
        }
        String newPassword = PasswordUtil.generateRandomPassword();
        userDAO.updatePassword(userId, passwordEncoder.encode(newPassword));
        log.info("管理员重置用户密码 userId={}", userId);
        // TODO: 发送邮件通知用户新密码
        return new PasswordResetVO().setNewPassword(newPassword);
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
        List<UserInfoDO> userInfoList = userInfoDAO.listByUserIds(userIds);
        boolean hasAdmin = userInfoList.stream().anyMatch(info -> UserRoleEnum.ADMIN.equals(info.getUserRole()));
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
        IPage<UserDetailDTO> resultPage = userDAO.pageUsers(query);

        List<UserListVO> voList = resultPage.getRecords().stream().map(dto -> {
            UserListVO vo = userConverter.toListVO(dto);
            vo.setAvatar(fileUrlHelper.buildUrl(vo.getAvatar()));
            return vo;
        }).toList();

        PageVO<UserListVO> pageVO = new PageVO<>();
        pageVO.setPageNum(resultPage.getCurrent());
        pageVO.setPageSize(resultPage.getSize());
        pageVO.setTotalElements(resultPage.getTotal());
        pageVO.setTotalPages(resultPage.getPages());
        pageVO.setHasPrevious(resultPage.getCurrent() > 1);
        pageVO.setHasNext(resultPage.getCurrent() < resultPage.getPages());
        pageVO.setContent(voList);
        return pageVO;
    }
}
