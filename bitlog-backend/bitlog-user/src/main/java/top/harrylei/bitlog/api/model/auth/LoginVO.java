package top.harrylei.bitlog.api.model.auth;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

/**
 * 登录/刷新 Token 响应
 *
 * @author harry
 * @since 0.0.1
 */
@Data
@NoArgsConstructor
@AllArgsConstructor
public class LoginVO {

    /**
     * 访问令牌（15 分钟有效）
     */
    private String accessToken;
}
