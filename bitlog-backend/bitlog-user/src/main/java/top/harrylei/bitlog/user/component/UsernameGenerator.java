package top.harrylei.bitlog.user.component;

import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;
import top.harrylei.bitlog.common.enums.ResultCode;
import top.harrylei.bitlog.user.repository.dao.UserDAO;

import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.ThreadLocalRandom;

/**
 * 第三方登录首次建号时为用户生成一个可用的用户名
 *
 * @author Harry
 * @since 2026-07-31
 */
@Component
@RequiredArgsConstructor
public class UsernameGenerator {

    private static final String ILLEGAL_CHARS = "[^\\u4e00-\\u9fffa-zA-Z0-9_-]";
    private static final String FALLBACK_BASE = "user";
    private static final int MIN_LENGTH = 2;
    private static final int MAX_LENGTH = 16;
    private static final int SUFFIX_ORIGIN = 1000;
    private static final int SUFFIX_BOUND = 10000;
    private static final int SUFFIX_LENGTH = 4;
    private static final int MAX_ATTEMPTS = 5;

    private final UserDAO userDAO;

    /**
     * 依次尝试第三方昵称、邮箱前缀，都不可用时退化为带随机后缀的名字。 返回值仍可能因并发而撞唯一索引，调用方需捕获后重试。
     *
     * @param preferredName 第三方回传的显示名，Google 为 ID Token 的 name
     * @param email 第三方回传的邮箱
     * @return 生成时点上未被占用的用户名
     */
    public String generate(String preferredName, String email) {
        List<String> candidates = buildCandidates(preferredName, email);
        for (String candidate : candidates) {
            if (!userDAO.isUsernameTaken(candidate)) {
                return candidate;
            }
        }

        String base = candidates.isEmpty() ? FALLBACK_BASE : candidates.getFirst();
        String prefix = truncate(base, MAX_LENGTH - SUFFIX_LENGTH);
        for (int i = 0; i < MAX_ATTEMPTS; i++) {
            String candidate = prefix + ThreadLocalRandom.current().nextInt(SUFFIX_ORIGIN, SUFFIX_BOUND);
            if (!userDAO.isUsernameTaken(candidate)) {
                return candidate;
            }
        }
        ResultCode.INTERNAL_ERROR.throwException("生成用户名失败");
        return null;
    }

    private List<String> buildCandidates(String preferredName, String email) {
        List<String> candidates = new ArrayList<>(2);
        addIfUsable(candidates, preferredName);
        addIfUsable(candidates, emailPrefix(email));
        return candidates;
    }

    private void addIfUsable(List<String> candidates, String raw) {
        String safe = raw == null ? "" : raw.trim().replaceAll(ILLEGAL_CHARS, "");
        String cleaned = truncate(safe, MAX_LENGTH);
        if (cleaned.length() >= MIN_LENGTH && !candidates.contains(cleaned)) {
            candidates.add(cleaned);
        }
    }

    private String emailPrefix(String email) {
        if (email == null) {
            return "";
        }
        int at = email.indexOf('@');
        return at > 0 ? email.substring(0, at) : email;
    }

    private String truncate(String value, int maxLength) {
        return value.length() <= maxLength ? value : value.substring(0, maxLength);
    }
}
