package top.harrylei.bitlog.user.converter;

import org.mapstruct.Mapper;
import org.mapstruct.Mapping;
import top.harrylei.bitlog.api.enums.user.UserStatusEnum;
import top.harrylei.bitlog.api.model.user.dto.UserDetailDTO;
import top.harrylei.bitlog.api.model.user.req.UserUpdateParam;
import top.harrylei.bitlog.api.model.user.vo.UserDetailVO;
import top.harrylei.bitlog.api.model.user.vo.UserListVO;
import top.harrylei.bitlog.api.model.user.vo.UserVO;
import top.harrylei.bitlog.user.repository.entity.UserDO;
import top.harrylei.bitlog.user.repository.entity.UserInfoDO;

/**
 * 用户对象转换器
 *
 * @author Harry
 * @since 2026-03-28
 */
@Mapper(componentModel = "spring")
public interface UserConverter {

    @Mapping(source = "userInfo.userId", target = "userId")
    @Mapping(source = "userInfo.nickname", target = "nickname")
    @Mapping(source = "userInfo.avatar", target = "avatar")
    @Mapping(source = "userInfo.profile", target = "profile")
    @Mapping(source = "userInfo.position", target = "position")
    @Mapping(source = "userInfo.company", target = "company")
    @Mapping(source = "userInfo.userRole", target = "userRole")
    @Mapping(source = "user.email", target = "email")
    UserVO toVO(UserInfoDO userInfo, UserDO user);

    @Mapping(source = "userId", target = "userId")
    @Mapping(source = "nickname", target = "nickname")
    @Mapping(source = "avatar", target = "avatar")
    @Mapping(source = "profile", target = "profile")
    @Mapping(source = "position", target = "position")
    @Mapping(source = "company", target = "company")
    @Mapping(source = "userRole", target = "userRole")
    @Mapping(target = "email", ignore = true)
    UserVO toVO(UserInfoDO userInfo);

    @Mapping(source = "userInfo.userId", target = "userId")
    @Mapping(source = "user.username", target = "username")
    @Mapping(source = "userInfo.nickname", target = "nickname")
    @Mapping(source = "userInfo.avatar", target = "avatar")
    @Mapping(source = "userInfo.profile", target = "profile")
    @Mapping(source = "userInfo.position", target = "position")
    @Mapping(source = "userInfo.company", target = "company")
    @Mapping(source = "userInfo.userRole", target = "userRole")
    @Mapping(source = "user.email", target = "email")
    @Mapping(source = "user.status", target = "status")
    @Mapping(source = "user.deleted", target = "deleted")
    @Mapping(source = "user.createTime", target = "createTime")
    @Mapping(source = "user.updateTime", target = "updateTime")
    UserDetailVO toDetailVO(UserInfoDO userInfo, UserDO user);

    @Mapping(source = "userInfo.userId", target = "userId")
    @Mapping(source = "user.username", target = "username")
    @Mapping(source = "userInfo.nickname", target = "nickname")
    @Mapping(source = "userInfo.avatar", target = "avatar")
    @Mapping(source = "userInfo.userRole", target = "userRole")
    @Mapping(source = "user.email", target = "email")
    @Mapping(source = "user.status", target = "status")
    @Mapping(source = "user.deleted", target = "deleted")
    @Mapping(source = "user.createTime", target = "createTime")
    @Mapping(source = "user.updateTime", target = "updateTime")
    UserListVO toListVO(UserInfoDO userInfo, UserDO user);

    @Mapping(source = "createTime", target = "createTime")
    @Mapping(source = "updateTime", target = "updateTime")
    UserListVO toListVO(UserDetailDTO dto);

    default Integer map(UserStatusEnum status) {
        return status == null ? null : status.getCode();
    }

    @Mapping(target = "id", ignore = true)
    @Mapping(target = "userId", ignore = true)
    @Mapping(target = "userRole", ignore = true)
    @Mapping(target = "avatar", ignore = true)
    @Mapping(target = "deleted", ignore = true)
    @Mapping(target = "createTime", ignore = true)
    @Mapping(target = "updateTime", ignore = true)
    UserInfoDO toInfoDO(UserUpdateParam req);
}
