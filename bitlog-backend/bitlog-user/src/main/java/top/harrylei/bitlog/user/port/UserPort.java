package top.harrylei.bitlog.user.port;

import java.util.List;
import top.harrylei.bitlog.user.model.vo.UserVO;

/**
 * 用户模块对外契约，其余模块只应依赖本接口，不直接注入领域服务
 *
 * @author Harry
 * @since 2026-08-09
 */
public interface UserPort {

    /**
     * 查询全部启用中的管理员 ID
     *
     * @return 管理员用户 ID 列表
     */
    List<Long> listAdminIds();

    /**
     * 批量获取用户基础信息，已注销账号以占位身份返回
     *
     * @param userIds 用户 ID 列表
     * @return 用户基础信息列表
     */
    List<UserVO> getUserBatchByIds(List<Long> userIds);
}
