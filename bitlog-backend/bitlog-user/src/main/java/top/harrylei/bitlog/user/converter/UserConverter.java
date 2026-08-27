package top.harrylei.bitlog.user.converter;

import org.mapstruct.Mapper;
import org.mapstruct.Mapping;
import top.harrylei.bitlog.user.model.enums.UserStatusEnum;
import top.harrylei.bitlog.user.model.dto.UserAccountDTO;
import top.harrylei.bitlog.user.model.dto.UserDetailDTO;
import top.harrylei.bitlog.user.model.vo.UserDetailVO;
import top.harrylei.bitlog.user.model.vo.UserListVO;
import top.harrylei.bitlog.user.model.vo.UserVO;
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
    @Mapping(source = "user.username", target = "username")
    @Mapping(source = "userInfo.avatar", target = "avatar")
    @Mapping(source = "userInfo.profile", target = "profile")
    @Mapping(source = "userInfo.position", target = "position")
    @Mapping(source = "userInfo.company", target = "company")
    @Mapping(source = "user.userRole", target = "userRole")
    @Mapping(source = "user.email", target = "email")
    UserVO toVO(UserInfoDO userInfo, UserDO user);

    @Mapping(source = "userInfo.userId", target = "userId")
    @Mapping(source = "user.username", target = "username")
    @Mapping(source = "userInfo.avatar", target = "avatar")
    @Mapping(source = "userInfo.profile", target = "profile")
    @Mapping(source = "userInfo.position", target = "position")
    @Mapping(source = "userInfo.company", target = "company")
    @Mapping(source = "user.userRole", target = "userRole")
    @Mapping(source = "user.email", target = "email")
    @Mapping(source = "user.status", target = "status")
    @Mapping(source = "user.deleted", target = "deleted")
    @Mapping(source = "user.createTime", target = "createTime")
    @Mapping(source = "user.updateTime", target = "updateTime")
    // 密码本身不出站，由 Service 换算成布尔量后回填
    @Mapping(target = "hasPassword", ignore = true)
    UserDetailVO toDetailVO(UserInfoDO userInfo, UserDO user);

    @Mapping(source = "userInfo.userId", target = "userId")
    @Mapping(source = "user.username", target = "username")
    @Mapping(source = "userInfo.avatar", target = "avatar")
    @Mapping(source = "user.userRole", target = "userRole")
    @Mapping(source = "user.email", target = "email")
    @Mapping(source = "user.status", target = "status")
    @Mapping(source = "user.deleted", target = "deleted")
    @Mapping(source = "user.createTime", target = "createTime")
    @Mapping(source = "user.updateTime", target = "updateTime")
    UserListVO toListVO(UserInfoDO userInfo, UserDO user);

    @Mapping(source = "createTime", target = "createTime")
    @Mapping(source = "updateTime", target = "updateTime")
    UserListVO toListVO(UserDetailDTO dto);

    @Mapping(source = "id", target = "userId")
    UserAccountDTO toAccountDTO(UserDO user);

    default Integer map(UserStatusEnum status) {
        return status == null ? null : status.getCode();
    }
}
