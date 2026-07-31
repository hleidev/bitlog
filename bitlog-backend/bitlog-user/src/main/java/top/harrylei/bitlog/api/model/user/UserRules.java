package top.harrylei.bitlog.api.model.user;

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

    private UserRules() {}
}
