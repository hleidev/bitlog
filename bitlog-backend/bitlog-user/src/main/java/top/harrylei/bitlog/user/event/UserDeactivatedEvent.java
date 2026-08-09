package top.harrylei.bitlog.user.event;

/**
 * 账号已注销
 * <p>
 * 由用户侧在墓碑化事务内发布，认证侧同步监听清理第三方绑定与会话。 用事件而非直接调用，是为了让用户侧不必依赖认证侧的组件——依赖方向单向由认证指向用户。
 * </p>
 *
 * @author Harry
 * @since 2026-08-09
 */
public record UserDeactivatedEvent(Long userId) {
}
