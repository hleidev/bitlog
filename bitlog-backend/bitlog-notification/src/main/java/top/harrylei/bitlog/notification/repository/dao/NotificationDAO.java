package top.harrylei.bitlog.notification.repository.dao;

import com.baomidou.mybatisplus.extension.service.impl.ServiceImpl;
import org.springframework.stereotype.Repository;
import top.harrylei.bitlog.notification.repository.entity.NotificationDO;
import top.harrylei.bitlog.notification.repository.mapper.NotificationMapper;

/**
 * 通知数据访问对象
 *
 * @author Harry
 * @since 2026-09-06
 */
@Repository
public class NotificationDAO extends ServiceImpl<NotificationMapper, NotificationDO> {}
