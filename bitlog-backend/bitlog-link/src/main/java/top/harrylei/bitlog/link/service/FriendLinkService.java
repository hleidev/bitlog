package top.harrylei.bitlog.link.service;

import top.harrylei.bitlog.api.model.link.vo.FriendLinkVO;

import java.util.List;

/**
 * 友链服务
 *
 * @author Harry
 * @since 2026-08-14
 */
public interface FriendLinkService {

    /**
     * 查询公开展示的友链，新加入的排在前面
     *
     * @return 已通过审核的友链列表
     */
    List<FriendLinkVO> listApproved();
}
