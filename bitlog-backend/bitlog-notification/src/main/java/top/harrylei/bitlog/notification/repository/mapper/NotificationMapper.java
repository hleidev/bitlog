package top.harrylei.bitlog.notification.repository.mapper;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import org.apache.ibatis.annotations.Mapper;
import top.harrylei.bitlog.notification.repository.entity.NotificationDO;

/**
 * 通知 Mapper
 *
 * @author Harry
 * @since 2026-09-06
 */
@Mapper
public interface NotificationMapper extends BaseMapper<NotificationDO> {

    /**
     * 幂等写入一条通知，命中 uk_notification_dedupe 时静默跳过
     *
     * @param notification 待写入的通知
     * @return 受影响行数，0 表示命中唯一约束被跳过
     */
    int insertIgnoreDuplicate(NotificationDO notification);
}
