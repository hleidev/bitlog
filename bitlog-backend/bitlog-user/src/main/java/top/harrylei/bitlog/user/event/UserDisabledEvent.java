package top.harrylei.bitlog.user.event;

import java.util.List;

/**
 * 账号已被封禁
 * <p>
 * 封禁后 Refresh Token 仍能续期，须由认证侧一并撤销。启用不发事件：被封期间会话早已清空。
 * </p>
 *
 * @author Harry
 * @since 2026-08-09
 */
public record UserDisabledEvent(List<Long> userIds) {
}
