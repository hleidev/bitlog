package top.harrylei.bitlog.link.repository.dao;

import com.baomidou.mybatisplus.core.metadata.IPage;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.baomidou.mybatisplus.extension.service.impl.ServiceImpl;
import org.springframework.stereotype.Repository;
import org.springframework.util.StringUtils;
import top.harrylei.bitlog.api.enums.link.FriendLinkStatusEnum;
import top.harrylei.bitlog.api.model.link.query.FriendLinkPageParam;
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
     * 公开展示的友链，新加入的排在前面
     */
    public List<FriendLinkDO> listApproved() {
        return lambdaQuery().eq(FriendLinkDO::getStatus, FriendLinkStatusEnum.APPROVED).orderByDesc(FriendLinkDO::getId)
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
        return lambdaQuery().eq(param.getStatus() != null, FriendLinkDO::getStatus, param.getStatus()).and(
            StringUtils.hasText(param.getKeyword()),
            w -> w.like(FriendLinkDO::getName, param.getKeyword()).or().like(FriendLinkDO::getUrl, param.getKeyword()))
            .page(page);
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
