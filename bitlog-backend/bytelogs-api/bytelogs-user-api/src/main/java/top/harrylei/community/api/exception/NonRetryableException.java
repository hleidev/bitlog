package top.harrylei.community.api.exception;

/**
 * 不可重试异常
 * 用于标识不应该重试的异常类型
 *
 * @author harry
 * @since 0.0.1
 */
public class NonRetryableException extends RuntimeException {

    public NonRetryableException(String message) {
        super(message);
    }

    public NonRetryableException(String message, Throwable cause) {
        super(message, cause);
    }
}
