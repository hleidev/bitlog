package top.harrylei.bitlog.auth.service;

/**
 * 登录/刷新操作的内部结果，包含双 Token
 *
 * @param accessToken  Access Token（15 分钟，返回给前端存内存）
 * @param refreshToken Refresh Token（30 天，由 Controller 写入 HttpOnly Cookie）
 *
 * @author harry
 * @since 0.0.1
 */
public record LoginResult(String accessToken, String refreshToken) {}
