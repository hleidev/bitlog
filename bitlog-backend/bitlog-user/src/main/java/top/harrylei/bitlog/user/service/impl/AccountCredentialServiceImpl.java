package top.harrylei.bitlog.user.service.impl;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.util.StringUtils;
import top.harrylei.bitlog.api.model.user.dto.UserAccountDTO;
import top.harrylei.bitlog.api.model.user.req.EmailCodeParam;
import top.harrylei.bitlog.api.model.user.req.EmailUpdateParam;
import top.harrylei.bitlog.api.model.user.req.PasswordInitParam;
import top.harrylei.bitlog.api.model.user.req.PasswordUpdateParam;
import top.harrylei.bitlog.api.model.user.vo.PasswordResetVO;
import top.harrylei.bitlog.api.model.user.vo.UserIdentityVO;
import top.harrylei.bitlog.common.constans.RedisKeyConstants;
import top.harrylei.bitlog.common.enums.ResultCode;
import top.harrylei.bitlog.common.util.EmailUtil;
import top.harrylei.bitlog.common.util.MaskUtil;
import top.harrylei.bitlog.user.component.RefreshTokenStore;
import top.harrylei.bitlog.user.component.VerificationCodeService;
import top.harrylei.bitlog.user.component.VerifyCodePurpose;
import top.harrylei.bitlog.user.repository.dao.UserIdentityDAO;
import top.harrylei.bitlog.user.repository.entity.UserIdentityDO;
import top.harrylei.bitlog.user.service.AccountCredentialService;
import top.harrylei.bitlog.user.service.UserAccountService;
import top.harrylei.bitlog.user.util.PasswordUtil;

import java.time.Duration;
import java.util.List;
import java.util.UUID;

/**
 * 账号凭据服务实现
 *
 * @author Harry
 * @since 2026-08-09
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class AccountCredentialServiceImpl implements AccountCredentialService {

    /** 够走完一次 Google 授权即可，留长了等于放大重放窗口 */
    private static final Duration BIND_INTENT_TTL = Duration.ofMinutes(5);

    private final UserAccountService userAccountService;
    private final UserIdentityDAO userIdentityDAO;
    private final PasswordEncoder passwordEncoder;
    private final RefreshTokenStore refreshTokenStore;
    private final VerificationCodeService verificationCodeService;
    private final StringRedisTemplate redisTemplate;

    @Override
    @Transactional(rollbackFor = Exception.class)
    public void updatePassword(Long userId, PasswordUpdateParam req, String currentRefreshToken) {
        UserAccountDTO user = requireAccount(userId);

        if (!passwordEncoder.matches(req.getOldPassword(), user.getPassword())) {
            throw ResultCode.ACCOUNT_OR_PASSWORD_ERROR.toException();
        }

        userAccountService.updatePassword(userId, passwordEncoder.encode(req.getNewPassword()));
        // 当前设备刚凭旧密码验过身份，保留；改密的意图是踢掉其余设备
        refreshTokenStore.revokeAll(userId, currentRefreshToken);
        log.info("用户修改密码 userId={}", userId);
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public void initPassword(Long userId, PasswordInitParam req, String currentRefreshToken) {
        UserAccountDTO user = requireAccount(userId);
        // 已有密码只能走带旧密码校验的修改流程，否则会话一旦被劫持即可静默改掉密码
        if (StringUtils.hasText(user.getPassword())) {
            throw ResultCode.OPERATION_NOT_ALLOWED.toException("已设置密码，请使用修改密码");
        }

        userAccountService.updatePassword(userId, passwordEncoder.encode(req.getPassword()));
        refreshTokenStore.revokeAll(userId, currentRefreshToken);
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
        UserAccountDTO user = requireAccount(userId);

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

        userAccountService.updateEmail(userId, newEmail);
        log.info("用户修改邮箱 userId={} email={}", userId, MaskUtil.email(newEmail));
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public PasswordResetVO resetPassword(Long userId) {
        requireAccount(userId);

        String newPassword = PasswordUtil.generateRandomPassword();
        userAccountService.updatePassword(userId, passwordEncoder.encode(newPassword));
        // 发起者是管理员，目标用户的会话一把都不该留
        refreshTokenStore.revokeAll(userId, null);
        log.info("管理员重置用户密码 userId={}", userId);
        // TODO: 发送邮件通知用户新密码
        return new PasswordResetVO().setNewPassword(newPassword);
    }

    /**
     * 校验新邮箱可用，返回归一化后的邮箱
     */
    private String checkEmailAvailable(Long userId, String email) {
        UserAccountDTO user = requireAccount(userId);
        String newEmail = EmailUtil.normalize(email);
        if (newEmail.equals(user.getEmail())) {
            ResultCode.INVALID_PARAMETER.throwException("新邮箱与当前邮箱相同");
        }
        if (userAccountService.isEmailTaken(newEmail)) {
            ResultCode.USER_ALREADY_EXISTS.throwException(newEmail);
        }
        return newEmail;
    }

    private UserAccountDTO requireAccount(Long userId) {
        UserAccountDTO user = userAccountService.getById(userId);
        if (user == null) {
            throw ResultCode.USER_NOT_EXISTS.toException();
        }
        return user;
    }
}
