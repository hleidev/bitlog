package top.harrylei.community.api.enums.user;

import lombok.AllArgsConstructor;
import lombok.Getter;

/**
 * 登录类型枚举
 *
 * @author harry
 * @since 0.0.1
 */
@Getter
@AllArgsConstructor
public enum LoginTypeEnum {

    USERNAME_PASSWORD(0),
    EMAIL_PASSWORD(1);

    private final int code;
}
