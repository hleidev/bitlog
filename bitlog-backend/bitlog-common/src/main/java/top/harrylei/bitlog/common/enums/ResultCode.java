package top.harrylei.bitlog.common.enums;

import lombok.Getter;
import top.harrylei.bitlog.common.exception.BusinessException;

/**
 * 全局响应码
 *
 * @author harry
 * @since 0.0.1
 */
@Getter
public enum ResultCode implements IResultCode {

    // ========== 通用 40xxx ==========
    INVALID_PARAMETER(40000, "参数错误"),
    TOKEN_INVALID(40001, "Token 无效或已过期"),
    FORBIDDEN(40003, "权限不足"),
    RESOURCE_NOT_FOUND(40004, "资源不存在"),
    METHOD_NOT_ALLOWED(40005, "请求方法不支持"),
    ALREADY_EXISTS(40009, "资源已存在"),
    OPERATION_NOT_ALLOWED(40010, "操作不被允许"),

    // ========== 认证 41xxx ==========
    PASSWORD_FORMAT_ERROR(41001, "密码格式不符合要求"),
    USERNAME_OR_PASSWORD_ERROR(41002, "用户名或密码错误"),

    // ========== 用户 42xxx ==========
    USER_NOT_EXISTS(42001, "用户不存在"),
    USER_ALREADY_EXISTS(42002, "用户已存在"),
    USER_DISABLED(42003, "用户已被禁用"),

    // ========== 系统 50xxx ==========
    INTERNAL_ERROR(50000, "系统内部错误"),
    SERVICE_UNAVAILABLE(50003, "服务暂不可用"),
    DATABASE_ERROR(50010, "数据库操作异常");

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
