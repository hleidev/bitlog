package top.harrylei.bitlog.user.model.dto;

import lombok.Data;
import lombok.experimental.Accessors;
import top.harrylei.bitlog.user.model.enums.UserRoleEnum;
import top.harrylei.bitlog.user.model.enums.UserStatusEnum;

/**
 * 账号主体 DTO，供认证侧读取凭据与账号状态
 * <p>
 * 内部传输专用：携带密码哈希，任何情况下都不得直接出站。
 * </p>
 *
 * @author Harry
 * @since 2026-08-09
 */
@Data
@Accessors(chain = true)
public class UserAccountDTO {

    private Long userId;

    private String username;

    private String email;

    /** Bcrypt 哈希，仅供认证侧比对；第三方登录建号时为空 */
    private String password;

    private UserStatusEnum status;

    private UserRoleEnum userRole;
}
