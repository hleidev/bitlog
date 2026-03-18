package top.harrylei.community.api.enums.response;

import lombok.Getter;
import top.harrylei.community.common.exception.BusinessException;

/**
 * 错误码枚举
 *
 * @author harry
 * @since 0.0.1
 */
@Getter
public enum ResultCode {
    // ========== 成功状态 ==========
    SUCCESS(0, "success"),

    // ========== 通用错误 4xxxx ==========
    INVALID_PARAMETER(40000, "参数错误"),
    AUTHENTICATION_FAILED(40001, "认证失败"),
    FORBIDDEN(40003, "权限不足"),
    RESOURCE_NOT_FOUND(40004, "资源不存在"),
    METHOD_NOT_ALLOWED(40005, "请求方法不支持"),
    RESOURCE_CONFLICT(40009, "资源已存在"),
    OPERATION_NOT_ALLOWED(40010, "操作不被允许"),

    // ========== 认证模块 41xxx ==========
    AUTH_PASSWORD_INVALID(41001, "密码格式不符合要求"),
    AUTH_LOGIN_FAILED(41002, "用户名或密码错误"),

    // ========== 用户模块 42xxx ==========
    USER_NOT_EXISTS(42001, "用户不存在"),
    USER_ALREADY_EXISTS(42002, "用户已存在"),
    USER_DISABLED(42003, "用户已被禁用"),

    // ========== 文章模块 43xxx ==========
    ARTICLE_NOT_EXISTS(43001, "文章不存在"),
    ARTICLE_ALREADY_EXISTS(43002, "文章已存在"),
    ARTICLE_CONTENT_INVALID(43003, "文章内容不符合规范"),

    // ========== 评论模块 44xxx ==========
    COMMENT_NOT_EXISTS(44001, "评论不存在"),
    COMMENT_CONTENT_INVALID(44002, "评论内容不符合规范"),

    // ========== 分类标签模块 45xxx ==========
    CATEGORY_NOT_EXISTS(45001, "分类不存在"),
    TAG_NOT_EXISTS(45002, "标签不存在"),
    TAG_ALREADY_EXISTS(45003, "标签已存在"),

    // ========== AI模块 46xxx ==========
    AI_MESSAGE_TOO_LONG(46001, "消息长度超过限制"),
    AI_HOURLY_LIMIT_EXCEEDED(46002, "每小时消息数量已达上限"),
    AI_DAILY_LIMIT_EXCEEDED(46003, "今日使用量已达上限"),
    AI_CONVERSATION_NOT_EXISTS(46004, "对话不存在或无访问权限"),
    AI_RESPONSE_EMPTY(46005, "AI响应为空"),
    AI_REPLY_CONTENT_EMPTY(46006, "AI回复内容为空"),
    AI_MODEL_NOT_SUPPORTED(46007, "不支持的AI厂商或模型"),

    // ========== 系统错误 5xxxx ==========
    INTERNAL_ERROR(50000, "系统内部错误"),
    SERVICE_UNAVAILABLE(50003, "服务暂不可用"),
    DATABASE_ERROR(50010, "数据库操作异常");

    private final int code;
    private final String message;

    ResultCode(int code, String message) {
        this.code = code;
        this.message = message;
    }

    /**
     * 直接抛出业务异常
     *
     * @param args 参数列表
     * @throws BusinessException 业务异常
     */
    public void throwException(Object... args) {
        String formattedMessage;
        if (args.length > 0) {
            StringBuilder sb = new StringBuilder(this.message);
            for (Object arg : args) {
                sb.append(": ").append(arg);
            }
            formattedMessage = sb.toString();
        } else {
            formattedMessage = this.message;
        }
        throw new BusinessException(this.code, formattedMessage);
    }

    /**
     * 是否为成功状态
     */
    public boolean isSuccess() {
        return this == SUCCESS;
    }

    /**
     * 根据状态码获取枚举
     */
    public static ResultCode getByCode(int code) {
        for (ResultCode resultCode : values()) {
            if (resultCode.getCode() == code) {
                return resultCode;
            }
        }
        return null;
    }
}
