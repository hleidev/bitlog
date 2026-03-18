package top.harrylei.community.api.enums.response;

import lombok.Getter;
import top.harrylei.community.common.enums.IResultCode;
import top.harrylei.community.common.exception.BusinessException;

/**
 * 用户服务响应码
 *
 * @author harry
 * @since 0.0.1
 */
@Getter
public enum ResultCode implements IResultCode {

    // ========== 认证模块 41xxx ==========
    AUTH_PASSWORD_INVALID(41001, "密码格式不符合要求"),
    AUTH_LOGIN_FAILED(41002, "用户名或密码错误"),

    // ========== 用户模块 42xxx ==========
    USER_NOT_EXISTS(42001, "用户不存在"),
    USER_ALREADY_EXISTS(42002, "用户已存在"),
    USER_DISABLED(42003, "用户已被禁用");

    private final int code;
    private final String message;

    ResultCode(int code, String message) {
        this.code = code;
        this.message = message;
    }

    public void throwException(Object... args) {
        String msg = this.message;
        if (args.length > 0) {
            StringBuilder sb = new StringBuilder(msg);
            for (Object arg : args) {
                sb.append(": ").append(arg);
            }
            msg = sb.toString();
        }
        throw new BusinessException(this.code, msg);
    }
}
