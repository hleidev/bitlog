package top.harrylei.bitlog.link.event;

/**
 * 友链已进入展示状态
 * <p>
 * 转存头像放在这一刻而非申请时：未过审的申请如果也转存，刷申请就等于往对象存储里灌垃圾， 审核是天然的闸门。
 * </p>
 *
 * @param linkId 友链 ID
 * @param avatarUrl 申请人填写的头像外链，可能为空
 * @param operatorId 执行审核或录入的管理员，仅用于拼对象存储的 key 路径
 * @author Harry
 * @since 2026-08-15
 */
public record FriendLinkApprovedEvent(Long linkId, String avatarUrl, Long operatorId) {
}
