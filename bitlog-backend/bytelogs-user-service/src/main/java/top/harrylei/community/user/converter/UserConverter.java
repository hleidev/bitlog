package top.harrylei.community.user.converter;

import org.mapstruct.Mapper;
import org.mapstruct.Mapping;
import top.harrylei.community.api.enums.user.UserStatusEnum;
import top.harrylei.community.api.model.user.req.UserUpdateRequest;
import top.harrylei.community.api.model.user.vo.UserDetailVO;
import top.harrylei.community.api.model.user.vo.UserListVO;
import top.harrylei.community.api.model.user.vo.UserVO;
import top.harrylei.community.user.repository.entity.UserDO;
import top.harrylei.community.user.repository.entity.UserInfoDO;

/**
 * 用户对象转换器
 *
 * @author harry
 * @since 0.0.1
 */
@Mapper(componentModel = "spring")
public interface UserConverter {

    /**
     * UserInfoDO + UserDO → UserVO（合并两个源）
     */
    @Mapping(source = "userInfo.userId", target = "userId")
    @Mapping(source = "userInfo.userName", target = "userName")
    @Mapping(source = "userInfo.avatar", target = "avatar")
    @Mapping(source = "userInfo.profile", target = "profile")
    @Mapping(source = "userInfo.position", target = "position")
    @Mapping(source = "userInfo.company", target = "company")
    @Mapping(source = "userInfo.userRole", target = "userRole")
    @Mapping(source = "user.email", target = "email")
    UserVO toVO(UserInfoDO userInfo, UserDO user);

    /**
     * UserInfoDO → UserVO（只有 info，email 为空）
     */
    @Mapping(source = "userId", target = "userId")
    @Mapping(source = "userName", target = "userName")
    @Mapping(source = "avatar", target = "avatar")
    @Mapping(source = "profile", target = "profile")
    @Mapping(source = "position", target = "position")
    @Mapping(source = "company", target = "company")
    @Mapping(source = "userRole", target = "userRole")
    @Mapping(target = "email", ignore = true)
    UserVO toVO(UserInfoDO userInfo);

    /**
     * UserInfoDO + UserDO → UserDetailVO（含基础信息）
     */
    @Mapping(source = "userInfo.userId", target = "userId")
    @Mapping(source = "userInfo.userName", target = "userName")
    @Mapping(source = "userInfo.avatar", target = "avatar")
    @Mapping(source = "userInfo.profile", target = "profile")
    @Mapping(source = "userInfo.position", target = "position")
    @Mapping(source = "userInfo.company", target = "company")
    @Mapping(source = "userInfo.userRole", target = "userRole")
    @Mapping(source = "userInfo.extend", target = "extend")
    @Mapping(source = "user.email", target = "email")
    @Mapping(source = "user.status", target = "status")
    @Mapping(source = "user.deleted", target = "deleted")
    @Mapping(source = "user.createTime", target = "createTime")
    @Mapping(source = "user.updateTime", target = "updateTime")
    UserDetailVO toDetailVO(UserInfoDO userInfo, UserDO user);

    /**
     * UserInfoDO + UserDO → UserListVO
     */
    @Mapping(source = "userInfo.userId", target = "userId")
    @Mapping(source = "userInfo.userName", target = "userName")
    @Mapping(source = "userInfo.avatar", target = "avatar")
    @Mapping(source = "userInfo.userRole", target = "userRole")
    @Mapping(source = "user.email", target = "email")
    @Mapping(source = "user.status", target = "status")
    @Mapping(source = "user.deleted", target = "deleted")
    @Mapping(source = "user.createTime", target = "createTime")
    @Mapping(source = "user.updateTime", target = "updateTime")
    UserListVO toListVO(UserInfoDO userInfo, UserDO user);

    /**
     * UserStatusEnum → Integer（供 MapStruct 自动使用）
     */
    default Integer map(UserStatusEnum status) {
        return status == null ? null : status.getCode();
    }

    /**
     * UserUpdateRequest → UserInfoDO（用于部分更新）
     */
    @Mapping(target = "id", ignore = true)
    @Mapping(target = "userId", ignore = true)
    @Mapping(target = "userRole", ignore = true)
    @Mapping(target = "deleted", ignore = true)
    @Mapping(target = "extend", ignore = true)
    @Mapping(target = "createTime", ignore = true)
    @Mapping(target = "updateTime", ignore = true)
    UserInfoDO toInfoDO(UserUpdateRequest req);
}
