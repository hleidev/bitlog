package top.harrylei.bitlog.common.mail;

import lombok.Data;
import org.springframework.boot.context.properties.ConfigurationProperties;

/**
 * 邮件发送配置
 *
 * @author Harry
 * @since 2026-07-30
 */
@Data
@ConfigurationProperties(prefix = "mail")
public class MailProperties {

    private String apiUrl;

    private String apiKey;

    private String from;

    private String fromName;

    /** 邮件页脚展示的站点地址，可为空 */
    private String siteUrl;
}
