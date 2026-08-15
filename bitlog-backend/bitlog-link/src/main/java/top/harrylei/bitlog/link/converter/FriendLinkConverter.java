package top.harrylei.bitlog.link.converter;

import org.mapstruct.Mapper;
import org.mapstruct.Mapping;
import org.mapstruct.MappingTarget;
import top.harrylei.bitlog.api.model.link.req.FriendLinkSaveParam;
import top.harrylei.bitlog.api.model.link.vo.FriendLinkAdminVO;
import top.harrylei.bitlog.api.model.link.vo.FriendLinkVO;
import top.harrylei.bitlog.api.model.link.vo.MyFriendLinkVO;
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

    /**
     * FriendLinkDO → MyFriendLinkVO（仅返回给申请人本人）
     */
    MyFriendLinkVO toMyVO(FriendLinkDO friendLink);

    /**
     * FriendLinkDO → FriendLinkAdminVO（申请人由服务层填充）
     */
    @Mapping(target = "applicant", ignore = true)
    FriendLinkAdminVO toAdminVO(FriendLinkDO friendLink);

    /**
     * 把提交内容覆盖到实体上
     * <p>
     * 归属、状态、拒绝理由由服务层判定，绝不能让请求体带进来。
     * </p>
     */
    @Mapping(target = "id", ignore = true)
    @Mapping(target = "userId", ignore = true)
    @Mapping(target = "status", ignore = true)
    @Mapping(target = "rejectReason", ignore = true)
    @Mapping(target = "createTime", ignore = true)
    @Mapping(target = "updateTime", ignore = true)
    void applyToEntity(FriendLinkSaveParam param, @MappingTarget FriendLinkDO target);
}
