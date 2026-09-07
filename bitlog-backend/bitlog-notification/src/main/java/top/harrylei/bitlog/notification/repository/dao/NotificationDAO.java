package top.harrylei.bitlog.notification.repository.dao;

import com.baomidou.mybatisplus.core.conditions.update.LambdaUpdateWrapper;
import com.baomidou.mybatisplus.core.metadata.IPage;
import com.baomidou.mybatisplus.core.toolkit.Wrappers;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.baomidou.mybatisplus.extension.service.impl.ServiceImpl;
import org.springframework.stereotype.Repository;
import top.harrylei.bitlog.common.enums.DeleteStatusEnum;
import top.harrylei.bitlog.notification.repository.entity.NotificationDO;
import top.harrylei.bitlog.notification.repository.mapper.NotificationMapper;

/**
 * 通知数据访问对象
 *
 * @author Harry
 * @since 2026-09-06
 */
@Repository
public class NotificationDAO extends ServiceImpl<NotificationMapper, NotificationDO> {

    /**
     * 分页查询某收件人的通知
     *
     * @param recipientId 收件人 ID
     * @param page 分页参数，排序由 BasePage 提供
     * @return 通知分页结果
     */
    public IPage<NotificationDO> pageByRecipient(Long recipientId, Page<NotificationDO> page) {
        return lambdaQuery().eq(NotificationDO::getRecipientId, recipientId)
            .eq(NotificationDO::getDeleted, DeleteStatusEnum.NOT_DELETED).page(page);
    }

    /**
     * 统计某收件人的未读通知数
     *
     * @param recipientId 收件人 ID
     * @return 未读数
     */
    public long countUnread(Long recipientId) {
        return lambdaQuery().eq(NotificationDO::getRecipientId, recipientId).isNull(NotificationDO::getReadTime)
            .eq(NotificationDO::getDeleted, DeleteStatusEnum.NOT_DELETED).count();
    }

    /**
     * 将某收件人名下的一条通知标记为已读，recipientId 写进 WHERE 条件以杜绝越权。 已读时刻用 COALESCE 保留首次已读值，重复标记同一条不报错，天然幂等
     *
     * @param notificationId 通知 ID
     * @param recipientId 收件人 ID
     * @return 受影响行数，0 表示通知不存在或不属于该收件人
     */
    public int markRead(Long notificationId, Long recipientId) {
        LambdaUpdateWrapper<NotificationDO> wrapper =
            Wrappers.<NotificationDO>lambdaUpdate().eq(NotificationDO::getId, notificationId)
                .eq(NotificationDO::getRecipientId, recipientId).setSql("read_time = COALESCE(read_time, now())");
        return getBaseMapper().update(null, wrapper);
    }

    /**
     * 将某收件人名下全部未读通知标记为已读
     *
     * @param recipientId 收件人 ID
     * @return 受影响行数
     */
    public int markAllRead(Long recipientId) {
        LambdaUpdateWrapper<NotificationDO> wrapper =
            Wrappers.<NotificationDO>lambdaUpdate().eq(NotificationDO::getRecipientId, recipientId)
                .isNull(NotificationDO::getReadTime).setSql("read_time = now()");
        return getBaseMapper().update(null, wrapper);
    }
}
