package top.harrylei.bitlog.user.service.impl;

import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import top.harrylei.bitlog.user.model.enums.UserRoleEnum;
import top.harrylei.bitlog.user.model.UserRules;
import top.harrylei.bitlog.user.model.dto.UserAccountDTO;
import top.harrylei.bitlog.common.enums.ResultCode;
import top.harrylei.bitlog.user.component.UsernameGenerator;
import top.harrylei.bitlog.user.converter.UserConverter;
import top.harrylei.bitlog.user.repository.dao.UserDAO;
import top.harrylei.bitlog.user.repository.dao.UserInfoDAO;
import top.harrylei.bitlog.user.repository.entity.UserDO;
import top.harrylei.bitlog.user.repository.entity.UserInfoDO;
import top.harrylei.bitlog.user.service.UserAccountService;

/**
 * 账号主体服务实现
 *
 * @author Harry
 * @since 2026-08-09
 */
@Service
@RequiredArgsConstructor
public class UserAccountServiceImpl implements UserAccountService {

    private final UserDAO userDAO;
    private final UserInfoDAO userInfoDAO;
    private final UserConverter userConverter;
    private final UsernameGenerator usernameGenerator;

    @Override
    public UserAccountDTO getById(Long userId) {
        return userConverter.toAccountDTO(userDAO.getById(userId));
    }

    @Override
    public UserAccountDTO getByEmail(String email) {
        return userConverter.toAccountDTO(userDAO.getByEmail(email));
    }

    @Override
    public boolean isEmailTaken(String email) {
        return userDAO.isEmailTaken(email);
    }

    @Override
    public void checkAccountAvailable(String email, String username) {
        if (userDAO.isEmailTaken(email)) {
            ResultCode.USER_ALREADY_EXISTS.throwException(email);
        }
        if (UserRules.isReserved(username) || userDAO.isUsernameTaken(username)) {
            ResultCode.USER_ALREADY_EXISTS.throwException(username);
        }
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public Long createAccount(String email, String username, String encodedPassword, UserRoleEnum role) {
        UserDO newUser =
            new UserDO().setUsername(username).setEmail(email).setPassword(encodedPassword).setUserRole(role);
        userDAO.save(newUser);

        UserInfoDO userInfo = new UserInfoDO().setUserId(newUser.getId()).setAvatar("");
        userInfoDAO.save(userInfo);
        return newUser.getId();
    }

    @Override
    public void updateProfile(Long userId, String profile, String position, String company) {
        userInfoDAO.updateInfo(userId, profile, position, company);
    }

    @Override
    public void updatePassword(Long userId, String encodedPassword) {
        userDAO.updatePassword(userId, encodedPassword);
    }

    @Override
    public void updateEmail(Long userId, String email) {
        userDAO.updateEmail(userId, email);
    }

    @Override
    public String generateUsername(String preferredName, String email) {
        return usernameGenerator.generate(preferredName, email);
    }
}
