package top.harrylei.bitlog.mail.port;

/**
 * 邮件发送失败异常，调用方据此判断发送是否成功
 *
 * @author Harry
 * @since 2026-09-06
 */
public class MailDeliveryException extends RuntimeException {

    public MailDeliveryException(String message, Throwable cause) {
        super(message, cause);
    }
}
