package top.harrylei.bitlog.link.service;

import top.harrylei.bitlog.api.model.link.query.FriendLinkPageParam;
import top.harrylei.bitlog.api.model.link.req.FriendLinkAuditParam;
import top.harrylei.bitlog.api.model.link.req.FriendLinkSaveParam;
import top.harrylei.bitlog.api.model.link.vo.FriendLinkAdminVO;
import top.harrylei.bitlog.api.model.link.vo.FriendLinkStatsVO;
import top.harrylei.bitlog.api.model.link.vo.FriendLinkVO;
import top.harrylei.bitlog.api.model.link.vo.MyFriendLinkVO;
import top.harrylei.bitlog.common.model.PageVO;

import java.util.List;

/**
 * 友链服务
 *
 * @author Harry
 * @since 2026-08-14
 */
public interface FriendLinkService {

    /**
     * 查询公开展示的友链，早加入的排在前面
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

    /**
     * 管理端分页查询
     *
     * @param param 查询参数
     * @return 分页结果
     */
    PageVO<FriendLinkAdminVO> pageForAdmin(FriendLinkPageParam param);

    /**
     * 管理端统计各状态的友链数量
     *
     * @return 状态计数
     */
    FriendLinkStatsVO getFriendLinkStats(FriendLinkPageParam param);

    /**
     * 站长手动录入友链，直接进入展示状态
     *
     * @param param 提交内容
     * @return 新建友链的 ID
     */
    Long saveByAdmin(FriendLinkSaveParam param);

    /**
     * 站长编辑友链内容
     *
     * @param id 友链 ID
     * @param param 提交内容
     */
    void updateByAdmin(Long id, FriendLinkSaveParam param);

    /**
     * 审核友链
     *
     * @param id 友链 ID
     * @param param 审核结果
     */
    void audit(Long id, FriendLinkAuditParam param);

    /**
     * 删除友链
     *
     * @param id 友链 ID
     */
    void deleteByAdmin(Long id);
}
