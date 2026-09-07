package top.harrylei.bitlog.user.port.impl;

import java.util.List;
import java.util.Map;
import java.util.function.Function;
import java.util.stream.Collectors;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import top.harrylei.bitlog.common.enums.DeleteStatusEnum;
import top.harrylei.bitlog.file.util.FileUrlHelper;
import top.harrylei.bitlog.user.converter.UserConverter;
import top.harrylei.bitlog.user.model.vo.UserVO;
import top.harrylei.bitlog.user.port.UserPort;
import top.harrylei.bitlog.user.repository.dao.UserDAO;
import top.harrylei.bitlog.user.repository.dao.UserInfoDAO;
import top.harrylei.bitlog.user.repository.entity.UserDO;
import top.harrylei.bitlog.user.repository.entity.UserInfoDO;

/**
 * 用户模块对外契约实现
 *
 * @author Harry
 * @since 2026-08-09
 */
@Service
@RequiredArgsConstructor
public class UserPortImpl implements UserPort {

    /** 注销后对外统一展示的用户名，真实用户名已被墓碑值覆写 */
    private static final String DEACTIVATED_USERNAME = "已注销用户";

    private final UserDAO userDAO;
    private final UserInfoDAO userInfoDAO;
    private final UserConverter userConverter;
    private final FileUrlHelper fileUrlHelper;

    @Override
    public List<Long> listAdminIds() {
        return userDAO.listAdminIds();
    }

    @Override
    public List<UserVO> getUserBatchByIds(List<Long> userIds) {
        if (userIds == null || userIds.isEmpty()) {
            return List.of();
        }
        List<UserInfoDO> userInfoList = userInfoDAO.listByUserIdsIncludingDeleted(userIds);
        if (userInfoList.isEmpty()) {
            return List.of();
        }

        List<Long> accountIds = userInfoList.stream().map(UserInfoDO::getUserId).toList();
        List<UserDO> userList = userDAO.listByUserIdsIncludingDeleted(accountIds);
        Map<Long, UserDO> userMap = userList.stream().collect(Collectors.toMap(UserDO::getId, Function.identity()));

        return userInfoList.stream()
                .map(info -> buildUserVO(info, userMap.get(info.getUserId())))
                .toList();
    }

    /** 注销账号的 deleted=1，唯有展示路径需要读到该行，故在此替换成占位身份 */
    private UserVO buildUserVO(UserInfoDO userInfo, UserDO user) {
        UserVO vo = userConverter.toVO(userInfo, user);
        boolean deactivated = user != null && DeleteStatusEnum.DELETED.equals(user.getDeleted());
        vo.setDeactivated(deactivated);
        if (deactivated) {
            vo.setUsername(DEACTIVATED_USERNAME);
        }
        vo.setAvatar(fileUrlHelper.buildUrl(vo.getAvatar()));
        return vo;
    }
}
