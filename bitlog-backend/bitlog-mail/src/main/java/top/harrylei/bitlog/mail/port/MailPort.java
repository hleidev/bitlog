package top.harrylei.bitlog.mail.port;

/**
 * 邮件发送的跨模块契约
 *
 * @author Harry
 * @since 2026-09-06
 */
public interface MailPort {

    /**
     * 同步发送一封邮件，是否异步由调用方决定
     *
     * @param to 收件地址
     * @param subject 邮件主题
     * @param html HTML 正文
     * @param text 纯文本正文
     * @param logLabel 日志中用于标识该邮件类型的标签
     * @throws MailDeliveryException 发送失败时抛出
     */
    void send(String to, String subject, String html, String text, String logLabel);
}
