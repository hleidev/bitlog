package top.harrylei.bitlog.common.exception;

import lombok.Getter;

/**
 * 业务异常类
 *
 * @author Harry
 * @since 2026-03-17
 */
@Getter
public class BusinessException extends RuntimeException {

    /**
     * 错误码
     */
    private final int code;

    /**
     * 错误消息
     */
    private final String message;

    public BusinessException(int code, String message) {
        super(message);
        this.code = code;
        this.message = message;
    }

    @Override
    public String toString() {
        return String.format("BusinessException[%d]: %s", code, message);
    }
}
