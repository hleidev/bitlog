package top.harrylei.bitlog.user.model.enums;

/**
 * 账号状态过滤维度
 * <p>
 * 业务上账号是三个终态，存储上由 status + deleted 两列表达，翻译由 UserPageParam 承担。 不落库，故不提供 fromCode，按名称绑定
 *
 * @author Harry
 * @since 2026-08-06
 */
public enum UserStateEnum {

    /** 启用 */
    ENABLED,

    /** 禁用 */
    DISABLED,

    /** 已注销，注销不改 status，只置 deleted */
    DEACTIVATED
}
