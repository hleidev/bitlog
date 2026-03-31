package top.harrylei.bitlog.api.feign;

import org.springframework.cloud.openfeign.FeignClient;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestParam;
import top.harrylei.bitlog.common.model.Result;
import top.harrylei.bitlog.api.model.user.vo.UserVO;

import java.util.List;

/**
 * 用户服务远程调用接口
 * <p>
 * 供其他微服务调用，用于获取用户基础信息。
 * 调用方引入 bitlog-user-api 依赖后即可注入使用。
 * </p>
 *
 * @author harry
 * @since 0.0.1
 */
@FeignClient(name = "bitlog-user-service", path = "/api/v1/internal/user")
public interface UserClient {

    /**
     * 根据用户 ID 查询用户基础信息
     *
     * @param userId 用户 ID
     * @return 用户信息
     */
    @GetMapping("/{userId}")
    Result<UserVO> getUserInfo(@PathVariable Long userId);

    /**
     * 批量查询用户基础信息
     *
     * @param userIds 用户 ID 列表
     * @return 用户信息列表
     */
    @GetMapping("/batch")
    Result<List<UserVO>> getUserInfoBatch(@RequestParam List<Long> userIds);
}
