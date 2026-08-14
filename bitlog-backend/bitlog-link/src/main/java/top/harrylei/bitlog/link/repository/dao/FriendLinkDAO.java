package top.harrylei.bitlog.link.repository.dao;

import com.baomidou.mybatisplus.extension.service.impl.ServiceImpl;
import org.springframework.stereotype.Repository;
import top.harrylei.bitlog.api.enums.link.FriendLinkStatusEnum;
import top.harrylei.bitlog.common.enums.DeleteStatusEnum;
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
        return lambdaQuery().eq(FriendLinkDO::getStatus, FriendLinkStatusEnum.APPROVED)
            .eq(FriendLinkDO::getDeleted, DeleteStatusEnum.NOT_DELETED).orderByDesc(FriendLinkDO::getId).list();
    }

    /**
     * 取某个账号持有的友链，一个账号至多一条
     */
    public FriendLinkDO getByUserId(Long userId) {
        return lambdaQuery().eq(FriendLinkDO::getUserId, userId)
            .eq(FriendLinkDO::getDeleted, DeleteStatusEnum.NOT_DELETED).one();
    }

    /**
     * 站点地址是否已被别的友链占用，用于在撞唯一索引前给出明确报错
     */
    public FriendLinkDO getByUrl(String url) {
        return lambdaQuery().eq(FriendLinkDO::getUrl, url).eq(FriendLinkDO::getDeleted, DeleteStatusEnum.NOT_DELETED)
            .one();
    }
}
