package top.harrylei.community.common.exception;

/**
 * 可重试异常
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
