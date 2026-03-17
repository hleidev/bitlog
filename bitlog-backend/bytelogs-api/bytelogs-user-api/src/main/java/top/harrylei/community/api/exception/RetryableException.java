package top.harrylei.community.api.exception;

/**
 * 可重试异常
 * 用于标识可以重试的异常类型
 *
 * @author harry
 * @since 0.0.1
 */
public class RetryableException extends RuntimeException {

    public RetryableException(String message) {
        super(message);
    }

    public RetryableException(String message, Throwable cause) {
        super(message, cause);
    }
}
