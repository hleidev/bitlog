package top.harrylei.bitlog.link.repository.dao;

import com.baomidou.mybatisplus.extension.service.impl.ServiceImpl;
import org.springframework.stereotype.Repository;
import top.harrylei.bitlog.api.enums.link.FriendLinkStatusEnum;
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
     * 覆盖自助修改可写的字段
     * <p>
     * 不能用 updateById：MyBatis-Plus 默认 NOT_NULL 更新策略会把值为 null 的列从 SET 子句里剔掉， 于是清空头像、清空简介、以及重新提交时清除拒绝理由全都会静默失效。 逐列显式 set
     * 顺带框死了自助路径能改哪些列，userId 碰不到。
     * </p>
     */
    public boolean updateSelfService(FriendLinkDO friendLink) {
        return lambdaUpdate().eq(FriendLinkDO::getId, friendLink.getId())
            .set(FriendLinkDO::getName, friendLink.getName()).set(FriendLinkDO::getUrl, friendLink.getUrl())
            .set(FriendLinkDO::getAvatar, friendLink.getAvatar())
            .set(FriendLinkDO::getDescription, friendLink.getDescription())
            .set(FriendLinkDO::getApplyMessage, friendLink.getApplyMessage())
            .set(FriendLinkDO::getStatus, friendLink.getStatus())
            .set(FriendLinkDO::getRejectReason, friendLink.getRejectReason()).update();
    }
}
