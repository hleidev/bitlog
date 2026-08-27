package top.harrylei.bitlog.user.model;

import java.util.Locale;
import java.util.Set;

/**
 * 用户字段校验规则，供注册、管理端建号与资料修改共用
 *
 * @author Harry
 * @since 2026-07-31
 */
public final class UserRules {

    /**
     * 用户名字符白名单：汉字、字母、数字、下划线、连字符。 用白名单而非黑名单，是为了挡住西里尔字母等同形字与零宽字符——二者都能构造出 与他人视觉完全一致、却能通过唯一索引的用户名，用于评论区冒充。
     */
    public static final String USERNAME_PATTERN = "^[\\u4e00-\\u9fffa-zA-Z0-9_-]{2,16}$";

    public static final String USERNAME_MESSAGE = "用户名为 2~16 位，可含汉字、字母、数字、下划线和连字符";

    /** 站点自身与管理身份的用词，留作保留字防止注册成官方号冒充 */
    private static final Set<String> RESERVED_USERNAMES = Set.of("admin", "administrator", "root", "system", "official",
        "support", "service", "api", "bitlog", "管理员", "官方", "系统", "客服");

    /** 注销墓碑用户名前缀，须整段保留，否则被抢注后注销时的覆写会撞 uk_username */
    public static final String DEACTIVATED_PREFIX = "del_";

    public static final String PASSWORD_PATTERN = "^[a-zA-Z0-9_@#%&!$*-]{8,20}$";

    /** 只描述字符集与长度，不宣称「必须包含字母数字」——正则并不做组成校验，写了会让排查走弯路 */
    public static final String PASSWORD_MESSAGE = "密码为 8~20 位，可含字母、数字及 _@#%&!$*- 符号";

    /** 登录只防超长输入，不校验格式：BCrypt 仅取前 72 字节，放任超长串徒增开销 */
    public static final int PASSWORD_MAX_INPUT = 128;

    /** 与 uk_username 的大小写不敏感保持一致，Admin 与 admin 同样拦下 */
    public static boolean isReserved(String username) {
        String normalized = username.toLowerCase(Locale.ROOT);
        return RESERVED_USERNAMES.contains(normalized) || normalized.startsWith(DEACTIVATED_PREFIX);
    }

    private UserRules() {}
}
