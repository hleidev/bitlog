package top.harrylei.community.common.exception;

import lombok.Getter;

/**
 * 业务异常类
 *
 * @author harry
 * @since 0.0.1
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
