package top.harrylei.bitlog.user.event;

/**
 * 第三方登录首次建号事件，携带平台侧头像地址
 *
 * @author Harry
 * @since 2026-08-01
 * @param userId 新建用户 ID
 * @param avatarUrl 平台回传的头像地址
 */
public record OAuthAvatarEvent(Long userId, String avatarUrl) {
}
