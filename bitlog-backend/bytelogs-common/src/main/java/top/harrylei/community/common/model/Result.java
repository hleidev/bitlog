package top.harrylei.community.common.model;

import lombok.Data;
import top.harrylei.community.common.enums.IResultCode;

import java.io.Serial;
import java.io.Serializable;

/**
 * 通用响应封装类
 *
 * @param <T> 返回结果的数据类型
 * @author harry
 * @since 0.0.1
 */
@Data
public class Result<T> implements Serializable {

    @Serial
    private static final long serialVersionUID = -510306209659393854L;

    private static final int SUCCESS_CODE = 0;
    private static final String SUCCESS_MSG = "success";

    private int code;

    private String message;

    private T data;

    public Result() {
    }

    public Result(int code, String message, T data) {
        this.code = code;
        this.message = message;
        this.data = data;
    }

    public static <T> Result<T> success(T data) {
        return new Result<>(SUCCESS_CODE, SUCCESS_MSG, data);
    }

    public static Result<Void> success() {
        return new Result<>(SUCCESS_CODE, SUCCESS_MSG, null);
    }

    public static <T> Result<T> fail(int code, String message) {
        return new Result<>(code, message, null);
    }

    public static <T> Result<T> fail(IResultCode resultCode, Object... args) {
        String message = formatMessage(resultCode.getMessage(), args);
        return fail(resultCode.getCode(), message);
    }

    private static String formatMessage(String message, Object... args) {
        if (args != null && args.length > 0) {
            return String.format(message, args);
        }
        return message;
    }
}
