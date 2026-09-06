package top.harrylei.bitlog.auth.service.impl;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.scheduling.annotation.Async;
import org.springframework.stereotype.Service;
import top.harrylei.bitlog.auth.mail.MailTemplates;
import top.harrylei.bitlog.auth.service.VerificationMailService;
import top.harrylei.bitlog.common.config.SiteProperties;
import top.harrylei.bitlog.common.util.MaskUtil;
import top.harrylei.bitlog.mail.port.MailDeliveryException;
import top.harrylei.bitlog.mail.port.MailPort;

import java.time.Duration;

/**
 * 基于 {@link MailPort} 组装并发送验证码邮件
 *
 * @author Harry
 * @since 2026-09-06
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class VerificationMailServiceImpl implements VerificationMailService {

    private final MailPort mailPort;
    private final SiteProperties siteProperties;

    @Async
    @Override
    public void sendVerificationCode(String to, String action, String code, Duration ttl) {
        String brand = siteProperties.getName();
        String siteUrl = siteProperties.displayHost();
        // 验证码前置于主题，用户在通知栏即可读到，无需打开邮件
        String subject = "%s 是你的 %s 验证码".formatted(code, brand);

        try {
            mailPort.send(to, subject, MailTemplates.verificationCodeHtml(brand, siteUrl, action, code, ttl),
                MailTemplates.verificationCodeText(brand, siteUrl, action, code, ttl), "验证码邮件-" + action);
        } catch (MailDeliveryException e) {
            log.error("验证码邮件发送失败 to={} action={}", MaskUtil.email(to), action, e);
        }
    }
}
