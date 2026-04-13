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
import top.harrylei.bitlog.api.model.user.dto.UserStatsDTO;
import top.harrylei.bitlog.api.model.user.query.UserPageQuery;
import top.harrylei.bitlog.api.model.user.req.PasswordUpdateRequest;
import top.harrylei.bitlog.api.model.user.req.UserUpdateRequest;
import top.harrylei.bitlog.api.model.user.vo.PasswordResetVO;
import top.harrylei.bitlog.api.model.user.vo.UserDetailVO;
import top.harrylei.bitlog.api.model.user.vo.UserListVO;
import top.harrylei.bitlog.api.model.user.vo.UserStatsVO;
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

import java.security.SecureRandom;
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

    private static final String PASSWORD_CHARS = "abcdefghijklmnopqrstuvwxyzABCDEFGHIJKLMNOPQRSTUVWXYZ0123456789";
    private static final int PASSWORD_LENGTH = 12;
    private static final SecureRandom SECURE_RANDOM = new SecureRandom();

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
        UserDO user = userDAO.getById(userInfo.getUserId());
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
        userInfoDAO.updateInfo(userId, req.getNickname(), req.getProfile(), req.getPosition(), req.getCompany());
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
        if (user == null) {
            ResultCode.USER_NOT_EXISTS.throwException();
        }

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
    public void updateUserStatusBatch(List<Long> userIds, UserStatusEnum status) {
        checkNotSelf(userIds);
        checkNotAdmin(userIds);
        userDAO.updateStatusBatch(userIds, status);
        log.info("批量修改用户状态 userIds={} status={}", userIds, status);
    }

    @Override
    public void deleteUserBatch(List<Long> userIds) {
        checkNotAdmin(userIds);
        userDAO.deleteBatch(userIds);
        log.info("批量软删除用户 userIds={}", userIds);
    }

    @Override
    public void restoreUserBatch(List<Long> userIds) {
        checkNotAdmin(userIds);
        userDAO.restoreBatch(userIds);
        log.info("批量恢复用户 userIds={}", userIds);
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public void removeUserBatch(List<Long> userIds) {
        checkNotAdmin(userIds);
        userDAO.removeBatch(userIds);
        userInfoDAO.removeByUserIds(userIds);
        log.info("批量物理删除用户 userIds={}", userIds);
    }

    @Override
    public UserStatsVO getUserStats() {
        UserStatsDTO dto = userDAO.countStats();
        UserStatsVO vo = new UserStatsVO();
        vo.setTotal(dto.getTotal());
        vo.setEnabled(dto.getEnabled());
        vo.setDisabled(dto.getDisabled());
        vo.setDeleted(dto.getDeleted());
        return vo;
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public PasswordResetVO resetPassword(Long userId) {
        UserDO user = userDAO.getById(userId);
        if (user == null) {
            ResultCode.USER_NOT_EXISTS.throwException();
        }
        String newPassword = generateRandomPassword();
        userDAO.updatePassword(userId, passwordEncoder.encode(newPassword));
        log.info("管理员重置用户密码 userId={}", userId);
        // TODO: 发送邮件通知用户新密码
        return new PasswordResetVO().setNewPassword(newPassword);
    }

    private void checkNotSelf(List<Long> userIds) {
        Long currentUserId = ReqInfoContext.getContext().getUserId();
        if (userIds.contains(currentUserId)) {
            ResultCode.OPERATION_NOT_ALLOWED.throwException("不能对自己的账号执行此操作");
        }
    }

    private void checkNotAdmin(List<Long> userIds) {
        // TODO: 后续支持分级管理员后，改为只拦同级或更高权限账号
        // TODO: 检查是否为最后一个管理员，防止系统失去管理员
        List<UserInfoDO> userInfoList = userInfoDAO.listByUserIds(userIds);
        boolean hasAdmin = userInfoList.stream()
                .anyMatch(info -> UserRoleEnum.ADMIN.equals(info.getUserRole()));
        if (hasAdmin) {
            ResultCode.OPERATION_NOT_ALLOWED.throwException("不能操作管理员账号");
        }
    }

    private String generateRandomPassword() {
        StringBuilder sb = new StringBuilder(PASSWORD_LENGTH);
        for (int i = 0; i < PASSWORD_LENGTH; i++) {
            sb.append(PASSWORD_CHARS.charAt(SECURE_RANDOM.nextInt(PASSWORD_CHARS.length())));
        }
        return sb.toString();
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
