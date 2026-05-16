package top.harrylei.bitlog.api.enums.user;

import com.baomidou.mybatisplus.annotation.EnumValue;
import lombok.AllArgsConstructor;
import lombok.Getter;

/**
 * 登录类型枚举
 *
 * @author Harry
 * @since 2026-03-17
 */
@Getter
@AllArgsConstructor
public enum LoginTypeEnum {

    USERNAME_PASSWORD(0), EMAIL_PASSWORD(1);

    @EnumValue
    private final int code;
}
