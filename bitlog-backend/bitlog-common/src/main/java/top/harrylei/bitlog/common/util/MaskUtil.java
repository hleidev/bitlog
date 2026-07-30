package top.harrylei.bitlog.common.util;

/**
 * 敏感信息脱敏工具，用于日志输出
 *
 * @author Harry
 * @since 2026-07-30
 */
public class MaskUtil {

    private MaskUtil() {
        throw new IllegalStateException("Utility class");
    }

    private static final int KEEP_PREFIX = 1;

    /**
     * 邮箱脱敏，保留首字符与域名，如 harry@example.com 输出 h***@example.com
     *
     * @param email 原始邮箱，可为 null
     * @return 脱敏结果，入参为空时原样返回
     */
    public static String email(String email) {
        if (email == null || email.isBlank()) {
            return email;
        }
        int at = email.indexOf('@');
        if (at <= KEEP_PREFIX) {
            return at < 0 ? "***" : "***" + email.substring(at);
        }
        return email.substring(0, KEEP_PREFIX) + "***" + email.substring(at);
    }
}
