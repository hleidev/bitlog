package top.harrylei.bitlog.auth.service;

import lombok.RequiredArgsConstructor;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.stereotype.Component;
import top.harrylei.bitlog.common.constants.RedisKeyConstants;
import top.harrylei.bitlog.common.context.ReqInfoContext;
import top.harrylei.bitlog.common.enums.ResultCode;
import top.harrylei.bitlog.common.util.RateLimiter;

import java.security.SecureRandom;
import java.time.Duration;
import java.util.List;
import java.util.concurrent.TimeUnit;

/**
 * 邮箱验证码的签发、校验与发信限流
 *
 * @author Harry
 * @since 2026-07-30
 */
@Component
@RequiredArgsConstructor
public class VerificationCodeService {

    private static final Duration CODE_TTL = Duration.ofMinutes(5);
    private static final int CODE_MAX_ATTEMPTS = 5;
    private static final int CODE_ORIGIN = 100000;
    private static final int CODE_BOUND = 1000000;
    private static final Duration COOLDOWN = Duration.ofSeconds(60);
    private static final Duration DAILY_WINDOW = Duration.ofHours(24);
    private static final Duration IP_WINDOW = Duration.ofHours(1);
    private static final int IP_MAX = 20;

    private static final SecureRandom SECURE_RANDOM = new SecureRandom();

    private final StringRedisTemplate redisTemplate;
    private final RateLimiter rateLimiter;
    private final VerificationMailService verificationMailService;

    /**
     * IP 维度发信限流。与邮箱无关，须在任何邮箱存在性判断之前调用， 否则「邮箱已注册」这类提前返回的分支将不受限流保护，可被用于无限枚举探测。
     */
    public void guardSendIp() {
        String clientIp = ReqInfoContext.getContext().getClientIp();
        if (clientIp != null) {
            rateLimiter.acquireOrThrow(RedisKeyConstants.getMailIpKey(clientIp), IP_MAX, IP_WINDOW);
        }
    }

    /**
     * 签发验证码并发送邮件。邮箱维度配额在此消耗，故只应在确定要发信时调用
     *
     * @param purpose 用途
     * @param email 目标邮箱，需已归一化
     */
    public void issueAndSend(VerifyCodePurpose purpose, String email) {
        rateLimiter.acquireOrThrow(RedisKeyConstants.getMailCooldownKey(purpose.getKey(), email), 1, COOLDOWN);
        rateLimiter.acquireOrThrow(RedisKeyConstants.getMailDailyKey(purpose.getKey(), email), purpose.getDailyMax(),
            DAILY_WINDOW);

        String code = String.valueOf(SECURE_RANDOM.nextInt(CODE_ORIGIN, CODE_BOUND));
        redisTemplate.opsForValue().set(RedisKeyConstants.getVerifyCodeKey(purpose.getKey(), email), code,
            CODE_TTL.toSeconds(), TimeUnit.SECONDS);

        verificationMailService.sendVerificationCode(email, purpose.getAction(), code, CODE_TTL);
    }

    /**
     * 校验并消费验证码。6 位数字熵仅 10^6，哈希无意义，靠失败次数上限防爆破： 累计错 {@value #CODE_MAX_ATTEMPTS} 次即作废该验证码。
     * 「未申请验证码」与「验证码填错」返回同一错误，否则可用于探测某邮箱是否正在走该流程。
     *
     * @param purpose 用途
     * @param email 目标邮箱，需已归一化
     * @param input 用户提交的验证码
     */
    public void verify(VerifyCodePurpose purpose, String email, String input) {
        String codeKey = RedisKeyConstants.getVerifyCodeKey(purpose.getKey(), email);
        String attemptsKey = RedisKeyConstants.getVerifyCodeAttemptsKey(purpose.getKey(), email);

        String stored = redisTemplate.opsForValue().get(codeKey);

        if (!rateLimiter.tryAcquire(attemptsKey, CODE_MAX_ATTEMPTS, CODE_TTL).allowed()) {
            redisTemplate.delete(List.of(codeKey, attemptsKey));
            ResultCode.VERIFY_CODE_INVALID.throwException("验证码错误次数过多，请重新获取");
        }
        if (stored == null || !stored.equals(input)) {
            ResultCode.VERIFY_CODE_INVALID.throwException();
        }

        redisTemplate.delete(List.of(codeKey, attemptsKey));
    }
}
