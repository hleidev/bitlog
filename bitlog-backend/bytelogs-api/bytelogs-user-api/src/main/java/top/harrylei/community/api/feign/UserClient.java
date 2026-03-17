package top.harrylei.community.api.feign;

import org.springframework.cloud.openfeign.FeignClient;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestParam;
import top.harrylei.community.api.model.base.Result;
import top.harrylei.community.api.model.user.vo.UserInfoVO;

import java.util.List;

/**
 * 用户服务远程调用接口
 * <p>
 * 供其他微服务调用，用于获取用户基础信息。
 * 调用方引入 bytelogs-user-api 依赖后即可注入使用。
 * </p>
 *
 * @author harry
 * @since 0.0.1
 */
@FeignClient(name = "bytelogs-user-service", path = "/api/v1/internal/user")
public interface UserClient {

    /**
     * 根据用户ID查询用户基础信息
     *
     * @param userId 用户ID
     * @return 用户信息
     */
    @GetMapping("/{userId}")
    Result<UserInfoVO> getUserInfo(@PathVariable("userId") Long userId);

    /**
     * 批量查询用户基础信息
     *
     * @param userIds 用户ID列表
     * @return 用户信息列表
     */
    @GetMapping("/batch")
    Result<List<UserInfoVO>> getUserInfoBatch(@RequestParam("userIds") List<Long> userIds);
}
