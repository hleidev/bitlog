package top.harrylei.bitlog.user.repository.entity;

import com.baomidou.mybatisplus.annotation.TableName;
import lombok.Data;
import lombok.EqualsAndHashCode;
import lombok.experimental.Accessors;
import top.harrylei.bitlog.common.enums.DeleteStatusEnum;
import top.harrylei.bitlog.api.enums.user.LoginTypeEnum;
import top.harrylei.bitlog.api.enums.user.UserStatusEnum;
import top.harrylei.bitlog.common.model.BaseDO;

import java.io.Serial;

/**
 * 用户账号实体
 *
 * @author Harry
 * @since 2026-03-18
 */
@Data
@EqualsAndHashCode(callSuper = true)
@TableName("user_account")
@Accessors(chain = true)
public class UserDO extends BaseDO {

    @Serial
    private static final long serialVersionUID = 1L;

    private String thirdAccountId;

    private LoginTypeEnum loginType;

    private DeleteStatusEnum deleted;

    private String username;

    private String password;

    private String email;

    private Boolean emailVerified;

    private UserStatusEnum status;
}
