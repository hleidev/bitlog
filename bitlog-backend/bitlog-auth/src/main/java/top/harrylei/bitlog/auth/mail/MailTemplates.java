package top.harrylei.bitlog.auth.mail;

import java.time.Duration;

/**
 * 邮件正文模板
 * <p>
 * 邮件客户端不支持外部样式表与现代布局：样式必须内联，布局须用 table， 且不能把关键信息放进图片（大量客户端默认屏蔽远程图片）。
 *
 * @author Harry
 * @since 2026-07-30
 */
public class MailTemplates {

    private MailTemplates() {
        throw new IllegalStateException("Utility class");
    }

    private static final String ACCENT = "#b05633";
    private static final String TEXT = "#1a1a1a";
    private static final String MUTED = "#6b6b6b";
    private static final String LINE = "#e5e5e5";
    private static final String BORDER = "#d4d4d4";
    private static final String SERIF = "Georgia,'Times New Roman','Songti SC',serif";
    private static final String MONO = "'SF Mono',SFMono-Regular,Menlo,Consolas,monospace";

    /**
     * 验证码邮件 HTML 正文
     *
     * @param brand 品牌名
     * @param siteUrl 站点地址，可为空
     * @param action 该验证码用于完成的动作，如「完成注册」
     * @param code 验证码
     * @param ttl 有效期
     * @return HTML 正文
     */
    public static String verificationCodeHtml(String brand, String siteUrl, String action, String code, Duration ttl) {
        String footerSite = siteUrl == null || siteUrl.isBlank() ? brand : brand + " · " + siteUrl;
        return """
            <div style="display:none;max-height:0;overflow:hidden;opacity:0;">验证码 %d 分钟内有效</div>
            <table role="presentation" width="100%%" cellpadding="0" cellspacing="0" border="0">
              <tr><td align="center" style="padding:16px;">
                <table role="presentation" width="600" cellpadding="0" cellspacing="0" border="0"
                       style="max-width:600px;width:100%%;font-family:%s;color:%s;">
                  <tr><td style="padding:8px 0 12px;font-size:18px;font-weight:bold;color:%s;">%s</td></tr>
                  <tr><td style="border-top:1px solid %s;font-size:0;line-height:0;">&nbsp;</td></tr>
                  <tr><td style="padding:28px 0 0;font-size:15px;line-height:1.7;">用于%s</td></tr>
                  <tr><td style="padding:18px 0 0;">
                    <table role="presentation" cellpadding="0" cellspacing="0" border="0"><tr>
                      <td style="border:1px solid %s;border-radius:4px;padding:14px 22px;
                                 font-family:%s;font-size:30px;letter-spacing:6px;color:%s;">%s</td>
                    </tr></table>
                  </td></tr>
                  <tr><td style="padding:20px 0 0;font-size:15px;line-height:1.7;">验证码 %d 分钟内有效，仅可使用一次。</td></tr>
                  <tr><td style="padding:16px 0 0;font-size:14px;line-height:1.7;color:%s;">
                    请勿将验证码转发或告知他人，任何人索要验证码都应视为诈骗。<br>
                    如果不是你本人操作，忽略这封邮件即可。
                  </td></tr>
                  <tr><td style="padding:28px 0 0;border-top:1px solid %s;font-size:0;line-height:0;">&nbsp;</td></tr>
                  <tr><td style="padding:14px 0 24px;font-size:13px;line-height:1.6;color:%s;">
                    %s<br>此邮件由系统自动发送，请勿回复。
                  </td></tr>
                </table>
              </td></tr>
            </table>""".formatted(ttl.toMinutes(), SERIF, TEXT, ACCENT, brand, LINE, action, BORDER, MONO, TEXT, code,
            ttl.toMinutes(), MUTED, LINE, MUTED, footerSite);
    }

    /**
     * 验证码邮件纯文本正文，与 HTML 一同作为 multipart 发送
     *
     * @param brand 品牌名
     * @param siteUrl 站点地址，可为空
     * @param action 该验证码用于完成的动作
     * @param code 验证码
     * @param ttl 有效期
     * @return 纯文本正文
     */
    public static String verificationCodeText(String brand, String siteUrl, String action, String code, Duration ttl) {
        String footerSite = siteUrl == null || siteUrl.isBlank() ? brand : brand + " · " + siteUrl;
        return """
            %s

            用于%s

            验证码：%s

            验证码 %d 分钟内有效，仅可使用一次。

            请勿将验证码转发或告知他人，任何人索要验证码都应视为诈骗。
            如果不是你本人操作，忽略这封邮件即可。

            --
            %s
            此邮件由系统自动发送，请勿回复。""".formatted(brand, action, code, ttl.toMinutes(), footerSite);
    }
}
