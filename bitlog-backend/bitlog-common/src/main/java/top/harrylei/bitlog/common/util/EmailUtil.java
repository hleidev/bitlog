package top.harrylei.bitlog.common.util;

import org.springframework.util.StringUtils;

import java.util.Locale;

/**
 * 邮箱归一化
 *
 * @author Harry
 * @since 2026-08-02
 */
public final class EmailUtil {

    private EmailUtil() {}

    /**
     * 归一化邮箱：去首尾空白并转小写。 注册、登录、改绑必须共用同一份实现——两处逻辑一旦出现分歧，就能用大小写变体绕过唯一性检查，在库里造出同一邮箱的多个账号。
     *
     * @param email 原始邮箱
     * @return 归一化结果，空白输入返回 null
     */
    public static String normalize(String email) {
        return StringUtils.hasText(email) ? email.trim().toLowerCase(Locale.ROOT) : null;
    }
}
