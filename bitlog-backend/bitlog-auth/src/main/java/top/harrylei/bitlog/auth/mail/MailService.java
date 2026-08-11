package top.harrylei.bitlog.auth.mail;

import java.time.Duration;

/**
 * 邮件发送服务
 *
 * @author Harry
 * @since 2026-07-30
 */
public interface MailService {

    /**
     * 异步发送验证码邮件，失败只记录日志不向上抛。 主题、HTML 与纯文本三者在此统一组装，调用方只需给出用途与验证码。
     *
     * @param to 收件地址
     * @param action 该验证码用于完成的动作，如「完成注册」
     * @param code 验证码
     * @param ttl 有效期
     */
    void sendVerificationCode(String to, String action, String code, Duration ttl);
}
