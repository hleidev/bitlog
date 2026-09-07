package top.harrylei.bitlog.link.event;

/**
 * 友链申请已提交事件
 *
 * @param linkId 友链 ID
 * @param applicantId 申请人 ID
 * @param linkName 站点名称
 * @param linkUrl 站点地址
 * @param applyMessage 申请留言
 * @author Harry
 * @since 2026-09-07
 */
public record FriendLinkAppliedEvent(
        Long linkId, Long applicantId, String linkName, String linkUrl, String applyMessage) {}
