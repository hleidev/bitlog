package top.harrylei.bitlog.common.enums;

import lombok.Getter;
import top.harrylei.bitlog.common.exception.BusinessException;

/**
 * 全局响应码
 *
 * @author Harry
 * @since 2026-03-18
 */
@Getter
public enum ResultCode implements IResultCode {

    // ========== 通用 40xxx ==========
    INVALID_PARAMETER(40000, "参数错误"), TOKEN_INVALID(40001, "Token 无效或已过期"), FORBIDDEN(40003, "权限不足"),
    RESOURCE_NOT_FOUND(40004, "资源不存在"), METHOD_NOT_ALLOWED(40005, "请求方法不支持"), ALREADY_EXISTS(40009, "资源已存在"),
    OPERATION_NOT_ALLOWED(40010, "操作不被允许"), RESOURCE_ALREADY_EXISTS(40011, "数据已存在，请勿重复提交"),

    // ========== 认证 41xxx ==========
    ACCOUNT_OR_PASSWORD_ERROR(41002, "账号或密码错误"), REFRESH_TOKEN_INVALID(41003, "Refresh Token 无效或已过期"),
    LOGIN_TOO_MANY_ATTEMPTS(41004, "登录失败次数过多，请稍后再试"), TOO_MANY_REQUESTS(41005, "操作过于频繁，请稍后再试"),
    VERIFY_CODE_INVALID(41006, "验证码无效或已过期"), OAUTH_EMAIL_UNVERIFIED(41007, "第三方账号的邮箱未验证"),

    // ========== 用户 42xxx ==========
    USER_NOT_EXISTS(42001, "用户不存在"), USER_ALREADY_EXISTS(42002, "用户已存在"), USER_DISABLED(42003, "用户已被禁用"),

    // ========== 文章 43xxx ==========
    ARTICLE_NOT_EXISTS(43001, "文章不存在"), ARTICLE_NOT_PUBLISHED(43002, "文章未发布"), ARTICLE_NO_PERMISSION(43003, "无权操作该文章"),
    ARTICLE_VERSION_NOT_EXISTS(43004, "文章版本不存在"), ARTICLE_CATEGORY_REQUIRED(43005, "发布前请先设置文章分类"),
    ARTICLE_NO_DRAFT_ABOVE_PUBLISH(43006, "没有未发布的草稿改动"), CATEGORY_NOT_EXISTS(43101, "分类不存在"),
    CATEGORY_ALREADY_EXISTS(43102, "分类已存在"), CATEGORY_HAS_ARTICLES(43104, "该分类下存在文章，请先移除文章"),
    TAG_NOT_EXISTS(43201, "标签不存在"), TAG_ALREADY_EXISTS(43202, "标签已存在"),

    // ========== 文件 44xxx ==========
    FILE_TYPE_NOT_ALLOWED(44001, "不支持的文件类型"), FILE_SIZE_EXCEEDED(44002, "文件大小超出限制"),

    // ========== AI 45xxx ==========
    AI_NOT_CONFIGURED(45001, "AI 功能未配置"), AI_SERVICE_ERROR(45002, "AI 服务调用失败"),

    // ========== 评论 46xxx ==========
    COMMENT_NOT_EXISTS(46001, "评论不存在"), COMMENT_NO_PERMISSION(46002, "无权操作该评论"),
    COMMENT_TOO_FREQUENT(46003, "评论过于频繁，请稍后再试"), COMMENT_NOT_ALLOWED(46004, "该文章当前不可评论"),

    // ========== 友链 47xxx ==========
    LINK_NOT_EXISTS(47001, "友链不存在"), LINK_ALREADY_APPLIED(47002, "已提交过友链申请"), LINK_URL_TAKEN(47003, "该站点已在友链中"),
    LINK_TOO_FREQUENT(47004, "操作过于频繁，请稍后再试"), LINK_STATUS_ILLEGAL(47005, "当前状态不允许该操作"),

    // ========== 通知 48xxx ==========
    NOTIFICATION_NOT_EXISTS(48001, "通知不存在"),

    // ========== 系统 50xxx ==========
    INTERNAL_ERROR(50000, "系统内部错误"), SERVICE_UNAVAILABLE(50003, "服务暂不可用"), DATABASE_ERROR(50010, "数据库操作异常");

    private final int code;
    private final String message;

    ResultCode(int code, String message) {
        this.code = code;
        this.message = message;
    }

    public BusinessException toException(Object... args) {
        String msg = this.message;
        if (args.length > 0) {
            StringBuilder sb = new StringBuilder(msg);
            for (Object arg : args) {
                sb.append(": ").append(arg);
            }
            msg = sb.toString();
        }
        return new BusinessException(this.code, msg);
    }

    public void throwException(Object... args) {
        throw toException(args);
    }
}
