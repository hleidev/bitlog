package top.harrylei.bitlog.link.event;

import top.harrylei.bitlog.link.model.enums.FriendLinkStatusEnum;

/**
 * 友链审核已完成事件
 *
 * @param linkId 友链 ID
 * @param ownerId 申请人 ID
 * @param operatorId 操作人 ID
 * @param status 审核结果
 * @param linkName 站点名称
 * @param rejectReason 拒绝理由
 * @author Harry
 * @since 2026-09-07
 */
public record FriendLinkReviewedEvent(
        Long linkId,
        Long ownerId,
        Long operatorId,
        FriendLinkStatusEnum status,
        String linkName,
        String rejectReason) {}
