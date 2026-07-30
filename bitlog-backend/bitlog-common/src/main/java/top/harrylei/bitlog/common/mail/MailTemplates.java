package top.harrylei.bitlog.common.mail;

import java.time.Duration;

/**
 * 邮件正文模板
 *
 * @author Harry
 * @since 2026-07-30
 */
public class MailTemplates {

    private MailTemplates() {
        throw new IllegalStateException("Utility class");
    }

    /**
     * 验证码邮件正文，有效期由调用方传入以保证与实际 TTL 一致
     *
     * @param action 该验证码用于完成的动作，如「完成注册」
     * @param code 验证码
     * @param ttl 有效期
     * @return HTML 正文
     */
    public static String verificationCode(String action, String code, Duration ttl) {
        return """
            <p>你的验证码是 <strong>%s</strong>，%d 分钟内有效。</p>
            <p>请在页面中输入该验证码以%s。</p>
            <p>如果不是你本人操作，忽略这封邮件即可。</p>""".formatted(code, ttl.toMinutes(), action);
    }
}
