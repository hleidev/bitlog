package top.harrylei.bitlog.link.service;

import top.harrylei.bitlog.api.model.link.req.FriendLinkSaveParam;
import top.harrylei.bitlog.api.model.link.vo.FriendLinkVO;
import top.harrylei.bitlog.api.model.link.vo.MyFriendLinkVO;

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

    /**
     * 查询本人持有的友链
     *
     * @param userId 当前用户
     * @return 友链，尚未申请时返回 null
     */
    MyFriendLinkVO getMine(Long userId);

    /**
     * 提交友链申请，一个账号至多持有一条
     *
     * @param userId 当前用户
     * @param param 提交内容
     * @return 新建友链的 ID
     */
    Long applyMine(Long userId, FriendLinkSaveParam param);

    /**
     * 修改本人持有的友链
     * <p>
     * 改动站点地址会使其重新进入待审核，其余字段立即生效。
     * </p>
     *
     * @param userId 当前用户
     * @param param 提交内容
     */
    void updateMine(Long userId, FriendLinkSaveParam param);

    /**
     * 撤回或删除本人持有的友链
     *
     * @param userId 当前用户
     */
    void deleteMine(Long userId);
}
