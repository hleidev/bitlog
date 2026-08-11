package top.harrylei.bitlog.auth.mail;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.MediaType;
import org.springframework.http.client.SimpleClientHttpRequestFactory;
import org.springframework.scheduling.annotation.Async;
import org.springframework.stereotype.Service;
import org.springframework.util.StringUtils;
import org.springframework.web.client.RestClient;
import top.harrylei.bitlog.common.util.MaskUtil;

import java.time.Duration;
import java.util.Map;

/**
 * 基于 Resend HTTP API 的邮件发送实现
 *
 * @author Harry
 * @since 2026-07-30
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class MailServiceImpl implements MailService {

    private final MailProperties mailProperties;
    private final RestClient restClient = buildRestClient();

    /** 必须设超时：默认无超时，服务端挂起会占死异步线程池，最终静默停止发信 */
    private static RestClient buildRestClient() {
        SimpleClientHttpRequestFactory factory = new SimpleClientHttpRequestFactory();
        factory.setConnectTimeout(5000);
        factory.setReadTimeout(10000);
        return RestClient.builder().requestFactory(factory).build();
    }

    @Async
    @Override
    public void sendVerificationCode(String to, String action, String code, Duration ttl) {
        String brand = mailProperties.getFromName();
        String siteUrl = mailProperties.getSiteUrl();
        // 验证码前置于主题，用户在通知栏即可读到，无需打开邮件
        String subject = "%s 是你的 %s 验证码".formatted(code, brand);

        doSend(to, subject, MailTemplates.verificationCodeHtml(brand, siteUrl, action, code, ttl),
            MailTemplates.verificationCodeText(brand, siteUrl, action, code, ttl), "验证码邮件-" + action);
    }

    /**
     * HTML 与纯文本同时发送：纯 HTML 邮件在部分客户端与过滤器上表现更差。 日志只记 logLabel 不记 subject——验证码前置在主题里，打主题等于把凭证写进日志。
     */
    private void doSend(String to, String subject, String html, String text, String logLabel) {
        if (!StringUtils.hasText(mailProperties.getApiKey())) {
            log.warn("未配置 mail.api-key，邮件未发送 to={} type={}", MaskUtil.email(to), logLabel);
            // 正文含验证码，仅 DEBUG 输出，避免生产漏配时把凭证写进日志
            log.debug("未发送的邮件正文 to={}\n{}", MaskUtil.email(to), text);
            return;
        }

        Map<String, Object> body =
            Map.of("from", "%s <%s>".formatted(mailProperties.getFromName(), mailProperties.getFrom()), "to",
                new String[] {to}, "subject", subject, "html", html, "text", text);

        try {
            restClient.post().uri(mailProperties.getApiUrl())
                .header("Authorization", "Bearer " + mailProperties.getApiKey()).contentType(MediaType.APPLICATION_JSON)
                .body(body).retrieve().toBodilessEntity();
            log.info("邮件发送成功 to={} type={}", MaskUtil.email(to), logLabel);
        } catch (Exception e) {
            log.error("邮件发送失败 to={} type={}", MaskUtil.email(to), logLabel, e);
        }
    }
}
