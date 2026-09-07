package top.harrylei.bitlog.notification.converter;

import org.mapstruct.Mapper;
import org.mapstruct.Mapping;
import top.harrylei.bitlog.notification.model.vo.NotificationActorVO;
import top.harrylei.bitlog.notification.model.vo.NotificationVO;
import top.harrylei.bitlog.notification.repository.entity.NotificationDO;
import top.harrylei.bitlog.user.model.vo.UserVO;

/**
 * 通知对象转换器
 *
 * @author Harry
 * @since 2026-09-07
 */
@Mapper(componentModel = "spring")
public interface NotificationConverter {

    /**
     * NotificationDO → NotificationVO（actor 由服务层填充）
     */
    @Mapping(target = "actor", ignore = true)
    NotificationVO toVO(NotificationDO notification);

    /**
     * UserVO → NotificationActorVO，仅保留通知展示所需字段
     */
    NotificationActorVO toActorVO(UserVO user);
}
