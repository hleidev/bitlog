package top.harrylei.bitlog.user.service.impl;

import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import top.harrylei.bitlog.file.service.FileService;
import top.harrylei.bitlog.file.util.FileUrlHelper;
import top.harrylei.bitlog.user.repository.dao.UserInfoDAO;
import top.harrylei.bitlog.user.repository.entity.UserInfoDO;

/**
 * 头像更新的重复请求与文件回收边界测试。
 *
 * @author Harry
 * @since 2026-09-11
 */
@ExtendWith(MockitoExtension.class)
class UserAvatarTest {
    @Mock
    private UserInfoDAO userInfoDAO;

    @Mock
    private FileUrlHelper fileUrlHelper;

    @Mock
    private FileService fileService;

    @InjectMocks
    private UserServiceImpl service;

    @Test
    void updateAvatar_sameKeyInDifferentForms_doesNotDeleteCurrentFile() {
        String key = "avatar/1/current.png";
        String url = "https://files.example.com/" + key;
        when(userInfoDAO.getByUserId(1L)).thenReturn(new UserInfoDO().setAvatar(key));
        when(fileUrlHelper.extractKey(url)).thenReturn(key);
        when(fileUrlHelper.extractKey(key)).thenReturn(key);

        service.updateAvatar(1L, url);
        service.updateAvatar(1L, url);

        verify(userInfoDAO, never()).updateAvatar(anyLong(), anyString());
        verifyNoInteractions(fileService);
    }

    @Test
    void updateAvatar_differentKey_updatesReferenceBeforeDeletingOldFile() {
        String oldKey = "avatar/1/old.png";
        String newKey = "avatar/1/new.png";
        when(userInfoDAO.getByUserId(1L)).thenReturn(new UserInfoDO().setAvatar(oldKey));
        when(fileUrlHelper.extractKey(oldKey)).thenReturn(oldKey);
        when(fileUrlHelper.extractKey(newKey)).thenReturn(newKey);

        service.updateAvatar(1L, newKey);

        var order = inOrder(userInfoDAO, fileService);
        order.verify(userInfoDAO).updateAvatar(1L, newKey);
        order.verify(fileService).delete(oldKey);
        verify(fileService, never()).delete(newKey);
    }
}
