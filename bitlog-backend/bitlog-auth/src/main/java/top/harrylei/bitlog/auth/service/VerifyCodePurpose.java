package top.harrylei.bitlog.auth.service;

import lombok.AllArgsConstructor;
import lombok.Getter;

/**
 * 邮箱验证码用途。每个用途独立计算 Redis 键与发信配额， 否则刷某一用途可耗尽对方当天额度，令其无法使用另一用途。
 *
 * @author Harry
 * @since 2026-07-30
 */
@Getter
@AllArgsConstructor
public enum VerifyCodePurpose {

    REGISTER("register", "完成注册", 5), RESET_PASSWORD("reset", "重置密码", 10), CHANGE_EMAIL("change_email", "修改邮箱", 5);

    /** Redis 键中的用途段 */
    private final String key;

    /** 邮件中描述该验证码用于完成什么 */
    private final String action;

    /** 单邮箱每日发信上限，找回密码放宽以免被恶意耗尽后当天无法自救 */
    private final int dailyMax;
}
