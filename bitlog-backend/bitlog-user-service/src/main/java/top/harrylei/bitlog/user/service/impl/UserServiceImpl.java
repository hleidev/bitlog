package top.harrylei.bitlog.user.service.impl;

import com.baomidou.mybatisplus.core.metadata.IPage;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import top.harrylei.bitlog.api.model.user.dto.UserDetailDTO;
import top.harrylei.bitlog.api.model.user.query.UserPageQuery;
import top.harrylei.bitlog.api.model.user.req.PasswordUpdateRequest;
import top.harrylei.bitlog.api.model.user.req.UserUpdateRequest;
import top.harrylei.bitlog.api.model.user.vo.UserDetailVO;
import top.harrylei.bitlog.api.model.user.vo.UserListVO;
import top.harrylei.bitlog.api.model.user.vo.UserVO;
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
        if (userId == null) {
            return null;
        }
        UserInfoDO userInfo = userInfoDAO.getByUserId(userId);
        if (userInfo == null) {
            return null;
        }
        UserDO user = userDAO.getById(userInfo.getId());
        if (user == null) {
            log.warn("用户账号数据缺失 userId={}", userId);
            return userConverter.toVO(userInfo);
        }
        return userConverter.toVO(userInfo, user);
    }

    @Override
    public List<UserVO> getUserBatchByIds(List<Long> userIds) {
        if (userIds == null || userIds.isEmpty()) {
            return List.of();
        }
        List<UserInfoDO> userInfoList = userInfoDAO.listByUserIds(userIds);
        if (userInfoList.isEmpty()) {
            return List.of();
        }

        // 批量查询对应的账号信息（user_info 的主键 id 对应 user_account 的主键 id）
        List<Long> accountIds = userInfoList.stream()
                .map(UserInfoDO::getId)
                .toList();
        List<UserDO> userList = userDAO.listByIds(accountIds);
        Map<Long, UserDO> userMap = userList.stream()
                .collect(Collectors.toMap(UserDO::getId, Function.identity()));

        return userInfoList.stream()
                .map(info -> {
                    UserDO user = userMap.get(info.getId());
                    if (user == null) {
                        log.warn("用户账号数据缺失 userId={}", info.getUserId());
                        return userConverter.toVO(info);
                    }
                    return userConverter.toVO(info, user);
                })
                .toList();
    }

    @Override
    public UserDetailVO getUserDetail(Long userId) {
        UserInfoDO userInfo = userInfoDAO.getByUserId(userId);
        if (userInfo == null) {
            ResultCode.USER_NOT_EXISTS.throwException();
        }
        UserDO user = userDAO.getById(userInfo.getId());
        if (user == null) {
            ResultCode.USER_NOT_EXISTS.throwException();
        }
        return userConverter.toDetailVO(userInfo, user);
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public void updateUserInfo(Long userId, UserUpdateRequest req) {
        UserInfoDO userInfo = userInfoDAO.getByUserId(userId);
        if (userInfo == null) {
            ResultCode.USER_NOT_EXISTS.throwException();
        }
        userInfoDAO.lambdaUpdate()
                .eq(UserInfoDO::getUserId, userId)
                .set(UserInfoDO::getUserName, req.getUserName())
                .set(req.getProfile() != null, UserInfoDO::getProfile, req.getProfile())
                .set(req.getPosition() != null, UserInfoDO::getPosition, req.getPosition())
                .set(req.getCompany() != null, UserInfoDO::getCompany, req.getCompany())
                .update();
        log.info("更新用户基本信息 userId={}", userId);
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public void updatePassword(Long userId, PasswordUpdateRequest req) {
        UserInfoDO userInfo = userInfoDAO.getByUserId(userId);
        if (userInfo == null) {
            ResultCode.USER_NOT_EXISTS.throwException();
        }
        UserDO user = userDAO.getById(userInfo.getId());
        if (user == null) {
            ResultCode.USER_NOT_EXISTS.throwException();
        }

        if (!passwordEncoder.matches(req.getOldPassword(), user.getPassword())) {
            ResultCode.USERNAME_OR_PASSWORD_ERROR.throwException();
        }

        userDAO.lambdaUpdate()
                .eq(UserDO::getId, user.getId())
                .set(UserDO::getPassword, passwordEncoder.encode(req.getNewPassword()))
                .update();
        log.info("用户修改密码 userId={}", userId);
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public void updateAvatar(Long userId, String avatar) {
        UserInfoDO userInfo = userInfoDAO.getByUserId(userId);
        if (userInfo == null) {
            ResultCode.USER_NOT_EXISTS.throwException();
        }
        userInfoDAO.lambdaUpdate()
                .eq(UserInfoDO::getUserId, userId)
                .set(UserInfoDO::getAvatar, avatar)
                .update();
        log.info("更新用户头像 userId={}", userId);
    }

    @Override
    public PageVO<UserListVO> pageQuery(UserPageQuery query) {
        IPage<UserDetailDTO> resultPage = userDAO.pageUsers(query, new Page<UserDetailDTO>(query.getPageNum(), query.getPageSize()));

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
