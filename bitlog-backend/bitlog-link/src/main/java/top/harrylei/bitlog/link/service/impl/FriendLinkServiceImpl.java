package top.harrylei.bitlog.link.service.impl;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import top.harrylei.bitlog.api.model.link.vo.FriendLinkVO;
import top.harrylei.bitlog.link.converter.FriendLinkConverter;
import top.harrylei.bitlog.link.repository.dao.FriendLinkDAO;
import top.harrylei.bitlog.link.service.FriendLinkService;

import java.util.List;

/**
 * 友链服务实现
 *
 * @author Harry
 * @since 2026-08-14
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class FriendLinkServiceImpl implements FriendLinkService {

    private final FriendLinkDAO friendLinkDAO;
    private final FriendLinkConverter friendLinkConverter;

    @Override
    public List<FriendLinkVO> listApproved() {
        return friendLinkConverter.toVOList(friendLinkDAO.listApproved());
    }
}
