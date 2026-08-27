package top.harrylei.bitlog.link.repository.dao;

import com.baomidou.mybatisplus.core.metadata.IPage;
import com.baomidou.mybatisplus.extension.conditions.query.LambdaQueryChainWrapper;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.baomidou.mybatisplus.extension.service.impl.ServiceImpl;
import org.springframework.stereotype.Repository;
import org.springframework.util.StringUtils;
import top.harrylei.bitlog.link.model.enums.FriendLinkStatusEnum;
import top.harrylei.bitlog.link.model.dto.FriendLinkStatsDTO;
import top.harrylei.bitlog.link.model.query.FriendLinkPageParam;
import top.harrylei.bitlog.link.repository.entity.FriendLinkDO;
import top.harrylei.bitlog.link.repository.mapper.FriendLinkMapper;

import java.util.List;

/**
 * 友链数据访问对象
 *
 * @author Harry
 * @since 2026-08-14
 */
@Repository
public class FriendLinkDAO extends ServiceImpl<FriendLinkMapper, FriendLinkDO> {

    /**
     * 公开展示的友链，早加入的排在前面
     * <p>
     * 老朋友占最显眼的位置，新朋友往后排。
     * </p>
     */
    public List<FriendLinkDO> listApproved() {
        return lambdaQuery().eq(FriendLinkDO::getStatus, FriendLinkStatusEnum.APPROVED).orderByAsc(FriendLinkDO::getId)
            .list();
    }

    /**
     * 取某个账号持有的友链，一个账号至多一条
     */
    public FriendLinkDO getByUserId(Long userId) {
        return lambdaQuery().eq(FriendLinkDO::getUserId, userId).one();
    }

    /**
     * 站点地址是否已被别的友链占用，用于在撞唯一约束前给出明确报错
     */
    public FriendLinkDO getByUrl(String url) {
        return lambdaQuery().eq(FriendLinkDO::getUrl, url).one();
    }

    /**
     * 管理端分页：状态精确匹配，关键词同时命中站点名称与地址
     */
    public IPage<FriendLinkDO> pageForAdmin(FriendLinkPageParam param, Page<FriendLinkDO> page) {
        return adminBaseQuery(param).eq(param.getStatus() != null, FriendLinkDO::getStatus, param.getStatus())
            .page(page);
    }

    /** 管理端列表与计数共用的基础过滤：只有关键词，不含状态。friend_link 是硬删，没有 deleted 列 */
    private LambdaQueryChainWrapper<FriendLinkDO> adminBaseQuery(FriendLinkPageParam param) {
        return lambdaQuery().and(StringUtils.hasText(param.getKeyword()),
            w -> w.like(FriendLinkDO::getName, param.getKeyword()).or().like(FriendLinkDO::getUrl,
                param.getKeyword()));
    }

    /**
     * 统计当前筛选下各状态的友链数量，供管理端 tab 计数使用
     * <p>
     * 与 pageForAdmin 共用 {@link #adminBaseQuery}，关键词口径必须一致， 否则搜索时 tab 数字会和列表行数对不上。
     * </p>
     *
     * @param param 查询条件，仅取关键词，状态分桶由本方法逐档统计
     * @return 状态计数
     */
    public FriendLinkStatsDTO countStats(FriendLinkPageParam param) {
        FriendLinkStatsDTO stats = new FriendLinkStatsDTO();
        stats.setTotal(adminBaseQuery(param).count());
        stats.setPending(adminBaseQuery(param).eq(FriendLinkDO::getStatus, FriendLinkStatusEnum.PENDING).count());
        stats.setApproved(adminBaseQuery(param).eq(FriendLinkDO::getStatus, FriendLinkStatusEnum.APPROVED).count());
        stats.setRejected(adminBaseQuery(param).eq(FriendLinkDO::getStatus, FriendLinkStatusEnum.REJECTED).count());
        return stats;
    }

    /**
     * 置状态并写入拒绝理由，审核与自助重新提交共用
     * <p>
     * 理由须显式 set：MyBatis-Plus 默认 NOT_NULL 更新策略会把 null 从 SET 子句里剔掉， 传 null 想清空反而会让上一次拒绝的说明留在库里，申请人下次被拒看到的是旧文案。
     * </p>
     */
    public boolean updateStatus(Long id, FriendLinkStatusEnum status, String rejectReason) {
        return lambdaUpdate().eq(FriendLinkDO::getId, id).set(FriendLinkDO::getStatus, status)
            .set(FriendLinkDO::getRejectReason, rejectReason).update();
    }

    /**
     * 头像转存完成后回写对象存储 key
     */
    public boolean updateAvatar(Long id, String avatarKey) {
        return lambdaUpdate().eq(FriendLinkDO::getId, id).set(FriendLinkDO::getAvatar, avatarKey).update();
    }

    /**
     * 解除友链的账号归属，降级为站长托管
     * <p>
     * 账号注销后本人已无法登录，友链不该跟着消失——那是站点之间的关系，不是账号的附属物。
     * </p>
     */
    public boolean detachOwner(Long userId) {
        return lambdaUpdate().eq(FriendLinkDO::getUserId, userId).set(FriendLinkDO::getUserId, null).update();
    }

    /**
     * 覆盖内容字段，自助修改与站长编辑共用
     * <p>
     * 只列内容列，不含 status 与 rejectReason：改简介不该顺带把审核状态写回去， 否则与并发的审核操作互相覆盖。归属列 userId 同样碰不到。
     * </p>
     * <p>
     * 同样不能用 updateById，理由见 {@link #updateStatus}。
     * </p>
     */
    public boolean updateContent(FriendLinkDO friendLink) {
        return lambdaUpdate().eq(FriendLinkDO::getId, friendLink.getId())
            .set(FriendLinkDO::getName, friendLink.getName()).set(FriendLinkDO::getUrl, friendLink.getUrl())
            .set(FriendLinkDO::getAvatar, friendLink.getAvatar())
            .set(FriendLinkDO::getDescription, friendLink.getDescription())
            .set(FriendLinkDO::getApplyMessage, friendLink.getApplyMessage()).update();
    }
}
