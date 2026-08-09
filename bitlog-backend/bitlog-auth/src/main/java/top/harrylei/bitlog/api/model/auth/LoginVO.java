package top.harrylei.bitlog.api.model.auth;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

/**
 * 登录/刷新 Token 响应
 *
 * @author Harry
 * @since 2026-04-09
 */
@Data
@NoArgsConstructor
@AllArgsConstructor
public class LoginVO {

    private String accessToken;
}
