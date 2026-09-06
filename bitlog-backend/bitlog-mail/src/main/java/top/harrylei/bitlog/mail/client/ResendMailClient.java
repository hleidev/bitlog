package top.harrylei.bitlog.mail.client;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.MediaType;
import org.springframework.http.client.SimpleClientHttpRequestFactory;
import org.springframework.stereotype.Component;
import org.springframework.util.StringUtils;
import org.springframework.web.client.RestClient;
import top.harrylei.bitlog.common.config.SiteProperties;
import top.harrylei.bitlog.common.util.MaskUtil;
import top.harrylei.bitlog.mail.config.MailProperties;
import top.harrylei.bitlog.mail.port.MailDeliveryException;
import top.harrylei.bitlog.mail.port.MailPort;

import java.util.Map;

/**
 * 基于 Resend HTTP API 的邮件发送实现
 *
 * @author Harry
 * @since 2026-09-06
 */
@Slf4j
@Component
@RequiredArgsConstructor
public class ResendMailClient implements MailPort {

    private final MailProperties mailProperties;
    private final SiteProperties siteProperties;
    private final RestClient restClient = buildRestClient();

    /** 必须设超时：默认无超时，服务端挂起会占死调用线程，最终静默停止发信 */
    private static RestClient buildRestClient() {
        SimpleClientHttpRequestFactory factory = new SimpleClientHttpRequestFactory();
        factory.setConnectTimeout(5000);
        factory.setReadTimeout(10000);
        return RestClient.builder().requestFactory(factory).build();
    }

    /**
     * 日志只记 logLabel 不记 subject：主题或正文可能含敏感内容，打印等于把敏感内容写进日志。
     */
    @Override
    public void send(String to, String subject, String html, String text, String logLabel) {
        if (!StringUtils.hasText(mailProperties.getApiKey())) {
            log.warn("未配置 mail.api-key，邮件未发送 to={} type={}", MaskUtil.email(to), logLabel);
            // 正文可能含敏感内容，仅 DEBUG 输出，避免生产漏配时把敏感内容写进日志
            log.debug("未发送的邮件正文 to={}\n{}", MaskUtil.email(to), text);
            return;
        }

        Map<String, Object> body =
            Map.of("from", "%s <%s>".formatted(siteProperties.getName(), mailProperties.getFrom()), "to",
                new String[] {to}, "subject", subject, "html", html, "text", text);

        try {
            restClient.post().uri(mailProperties.getApiUrl())
                .header("Authorization", "Bearer " + mailProperties.getApiKey()).contentType(MediaType.APPLICATION_JSON)
                .body(body).retrieve().toBodilessEntity();
            log.info("邮件发送成功 to={} type={}", MaskUtil.email(to), logLabel);
        } catch (Exception e) {
            throw new MailDeliveryException("邮件发送失败 type=" + logLabel, e);
        }
    }
}
