package top.harrylei.bitlog.common.mail;

/**
 * 邮件发送服务
 *
 * @author Harry
 * @since 2026-07-30
 */
public interface MailService {

    /**
     * 异步发送 HTML 邮件，失败只记录日志不向上抛
     *
     * @param to 收件地址
     * @param subject 主题
     * @param html 正文
     */
    void send(String to, String subject, String html);

}
