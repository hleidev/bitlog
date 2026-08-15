package top.harrylei.bitlog.link.converter;

import org.mapstruct.Mapper;
import top.harrylei.bitlog.api.model.link.vo.FriendLinkVO;
import top.harrylei.bitlog.link.repository.entity.FriendLinkDO;

import java.util.List;

/**
 * 友链对象转换器
 *
 * @author Harry
 * @since 2026-08-14
 */
@Mapper(componentModel = "spring")
public interface FriendLinkConverter {

    /**
     * FriendLinkDO → FriendLinkVO（公开）
     */
    FriendLinkVO toVO(FriendLinkDO friendLink);

    List<FriendLinkVO> toVOList(List<FriendLinkDO> friendLinks);
}
