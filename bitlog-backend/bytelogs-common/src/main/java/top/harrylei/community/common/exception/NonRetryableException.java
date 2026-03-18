package top.harrylei.community.common.exception;

/**
 * 不可重试异常
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
