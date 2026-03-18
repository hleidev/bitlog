package top.harrylei.community.common.enums;

import lombok.Getter;
import top.harrylei.community.common.exception.BusinessException;

/**
 * 通用响应码，适用于所有服务
 *
 * @author harry
 * @since 0.0.1
 */
@Getter
public enum CommonResultCode implements IResultCode {

    SUCCESS(0, "success"),

    // ========== 通用错误 40xxx ==========
    INVALID_PARAMETER(40000, "参数错误"),
    AUTHENTICATION_FAILED(40001, "认证失败"),
    FORBIDDEN(40003, "权限不足"),
    RESOURCE_NOT_FOUND(40004, "资源不存在"),
    METHOD_NOT_ALLOWED(40005, "请求方法不支持"),
    RESOURCE_CONFLICT(40009, "资源已存在"),
    OPERATION_NOT_ALLOWED(40010, "操作不被允许"),

    // ========== 系统错误 50xxx ==========
    INTERNAL_ERROR(50000, "系统内部错误"),
    SERVICE_UNAVAILABLE(50003, "服务暂不可用"),
    DATABASE_ERROR(50010, "数据库操作异常");

    private final int code;
    private final String message;

    CommonResultCode(int code, String message) {
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
