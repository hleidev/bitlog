package top.harrylei.bitlog.user.service.impl;

import com.baomidou.mybatisplus.core.metadata.IPage;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import top.harrylei.bitlog.api.enums.user.UserRoleEnum;
import top.harrylei.bitlog.api.enums.user.UserStatusEnum;
import top.harrylei.bitlog.api.model.user.dto.UserDetailDTO;
import top.harrylei.bitlog.api.model.user.query.UserPageQuery;
import top.harrylei.bitlog.api.model.user.req.PasswordUpdateRequest;
import top.harrylei.bitlog.api.model.user.req.UserUpdateRequest;
import top.harrylei.bitlog.api.model.user.vo.UserDetailVO;
import top.harrylei.bitlog.api.model.user.vo.UserListVO;
import top.harrylei.bitlog.api.model.user.vo.UserVO;
import top.harrylei.bitlog.common.context.ReqInfoContext;
import top.harrylei.bitlog.common.enums.ResultCode;
import top.harrylei.bitlog.common.model.PageVO;
import top.harrylei.bitlog.user.converter.UserConverter;
import top.harrylei.bitlog.user.repository.dao.UserDAO;
import top.harrylei.bitlog.user.repository.dao.UserInfoDAO;
import top.harrylei.bitlog.user.repository.entity.UserDO;
import top.harrylei.bitlog.user.repository.entity.UserInfoDO;
import top.harrylei.bitlog.user.service.UserService;

import java.util.List;
import java.util.Map;
import java.util.function.Function;
import java.util.stream.Collectors;

/**
 * 用户业务服务实现
 *
 * @author harry
 * @since 0.0.1
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class UserServiceImpl implements UserService {

    private final UserDAO userDAO;
    private final UserInfoDAO userInfoDAO;
    private final UserConverter userConverter;
    private final PasswordEncoder passwordEncoder;

    @Override
    public UserVO getUserById(Long userId) {
        UserInfoDO userInfo = userInfoDAO.getByUserId(userId);
        if (userInfo == null) {
            return null;
        }
        UserDO user = userDAO.getById(userInfo.getUserId());
        return userConverter.toVO(userInfo, user);
    }

    @Override
    public List<UserVO> getUserBatchByIds(List<Long> userIds) {
        if (userIds.isEmpty()) {
            return List.of();
        }
        List<UserInfoDO> userInfoList = userInfoDAO.listByUserIds(userIds);
        if (userInfoList.isEmpty()) {
            return List.of();
        }

        List<Long> accountIds = userInfoList.stream()
                .map(UserInfoDO::getUserId)
                .toList();
        List<UserDO> userList = userDAO.listByUserIds(accountIds);
        Map<Long, UserDO> userMap = userList.stream()
                .collect(Collectors.toMap(UserDO::getId, Function.identity()));

        return userInfoList.stream()
                .map(info -> userConverter.toVO(info, userMap.get(info.getUserId())))
                .toList();
    }

    @Override
    public UserDetailVO getUserDetail(Long userId) {
        UserInfoDO userInfo = userInfoDAO.getByUserId(userId);
        if (userInfo == null) {
            ResultCode.USER_NOT_EXISTS.throwException();
        }
        UserDO user = userDAO.getById(userInfo.getUserId());
        return userConverter.toDetailVO(userInfo, user);
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public void updateUserInfo(Long userId, UserUpdateRequest req) {
        UserInfoDO userInfo = userInfoDAO.getByUserId(userId);
        if (userInfo == null) {
            ResultCode.USER_NOT_EXISTS.throwException();
        }
        userInfoDAO.updateInfo(userId, req.getUserName(), req.getProfile(), req.getPosition(), req.getCompany());
        log.info("更新用户基本信息 userId={}", userId);
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public void updatePassword(Long userId, PasswordUpdateRequest req) {
        UserInfoDO userInfo = userInfoDAO.getByUserId(userId);
        if (userInfo == null) {
            ResultCode.USER_NOT_EXISTS.throwException();
        }
        UserDO user = userDAO.getById(userInfo.getUserId());

        if (!passwordEncoder.matches(req.getOldPassword(), user.getPassword())) {
            ResultCode.USERNAME_OR_PASSWORD_ERROR.throwException();
        }

        userDAO.updatePassword(user.getId(), passwordEncoder.encode(req.getNewPassword()));
        log.info("用户修改密码 userId={}", userId);
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public void updateAvatar(Long userId, String avatar) {
        UserInfoDO userInfo = userInfoDAO.getByUserId(userId);
        if (userInfo == null) {
            ResultCode.USER_NOT_EXISTS.throwException();
        }
        userInfoDAO.updateAvatar(userId, avatar);
        log.info("更新用户头像 userId={}", userId);
    }

    @Override
    public void updateUserStatus(Long userId, UserStatusEnum status) {
        if (userId.equals(ReqInfoContext.getContext().getUserId())) {
            ResultCode.OPERATION_NOT_ALLOWED.throwException("不能修改自己的账号状态");
        }
        UserDO user = userDAO.getById(userId);
        if (user == null) {
            ResultCode.USER_NOT_EXISTS.throwException();
        }
        UserInfoDO userInfo = userInfoDAO.getByUserId(userId);
        if (userInfo != null && UserRoleEnum.ADMIN.equals(userInfo.getUserRole())) {
            ResultCode.OPERATION_NOT_ALLOWED.throwException("不能修改管理员账号状态");
        }
        userDAO.updateStatus(userId, status);
        log.info("修改用户状态 userId={} status={}", userId, status);
    }

    @Override
    public PageVO<UserListVO> pageQuery(UserPageQuery query) {
        IPage<UserDetailDTO> resultPage = userDAO.pageUsers(query, new Page<>(query.getPageNum(), query.getPageSize()));

        List<UserListVO> voList = resultPage.getRecords().stream()
                .map(userConverter::toListVO)
                .toList();

        PageVO<UserListVO> pageVO = new PageVO<>();
        pageVO.setPageNum(resultPage.getCurrent());
        pageVO.setPageSize(resultPage.getSize());
        pageVO.setTotalElements(resultPage.getTotal());
        pageVO.setTotalPages(resultPage.getPages());
        pageVO.setHasPrevious(resultPage.getCurrent() > 1);
        pageVO.setHasNext(resultPage.getCurrent() < resultPage.getPages());
        pageVO.setContent(voList);
        return pageVO;
    }
}
