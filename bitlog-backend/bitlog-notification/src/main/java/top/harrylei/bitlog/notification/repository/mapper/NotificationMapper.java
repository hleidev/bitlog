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
public interface NotificationMapper extends BaseMapper<NotificationDO> {}
