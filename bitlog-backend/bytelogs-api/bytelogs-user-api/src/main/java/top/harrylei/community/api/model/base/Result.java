package top.harrylei.community.api.model.base;

import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Data;
import top.harrylei.community.api.enums.response.ResultCode;

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
@Schema(description = "统一响应结构")
public class Result<T> implements Serializable {
    @Serial
    private static final long serialVersionUID = -510306209659393854L;

    @Schema(description = "业务状态码，0表示成功，其他表示异常", requiredMode = Schema.RequiredMode.REQUIRED, example = "0")
    private int code;

    @Schema(description = "响应消息，成功时为'success'，失败时为具体错误信息", requiredMode = Schema.RequiredMode.REQUIRED, example = "success")
    private String message;

    @Schema(description = "业务数据，失败时可能为null")
    private T data;

    public Result() {
    }

    public Result(int code, String message, T data) {
        this.code = code;
        this.message = message;
        this.data = data;
    }

    public Result(T data) {
        this.code = ResultCode.SUCCESS.getCode();
        this.message = ResultCode.SUCCESS.getMessage();
        this.data = data;
    }

    public static <T> Result<T> success(T data) {
        return new Result<>(data);
    }

    public static Result<Void> success() {
        return new Result<>(ResultCode.SUCCESS.getCode(), ResultCode.SUCCESS.getMessage(), null);
    }

    public static <T> Result<T> fail(int code, String message) {
        return new Result<>(code, message, null);
    }

    public static <T> Result<T> fail(ResultCode status, Object... args) {
        String message = formatMessage(status.getMessage(), args);
        return fail(status.getCode(), message);
    }

    private static String formatMessage(String message, Object... args) {
        if (args != null && args.length > 0) {
            return String.format(message, args);
        }
        return message;
    }
}
