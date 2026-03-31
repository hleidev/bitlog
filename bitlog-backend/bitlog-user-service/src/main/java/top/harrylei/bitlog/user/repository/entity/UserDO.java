package top.harrylei.bitlog.user.repository.entity;

import com.baomidou.mybatisplus.annotation.TableName;
import lombok.Data;
import lombok.EqualsAndHashCode;
import lombok.experimental.Accessors;
import top.harrylei.bitlog.api.enums.common.DeleteStatusEnum;
import top.harrylei.bitlog.api.enums.user.LoginTypeEnum;
import top.harrylei.bitlog.api.enums.user.UserStatusEnum;
import top.harrylei.bitlog.common.model.BaseDO;

import java.io.Serial;

/**
 * 用户账号实体
 *
 * @author harry
 * @since 0.0.1
 */
@Data
@EqualsAndHashCode(callSuper = true)
@TableName("user_account")
@Accessors(chain = true)
public class UserDO extends BaseDO {

    @Serial
    private static final long serialVersionUID = 1L;

    /**
     * 第三方用户 ID
     */
    private String thirdAccountId;

    /**
     * 登录类型
     */
    private LoginTypeEnum loginType;

    /**
     * 删除标记
     */
    private DeleteStatusEnum deleted;

    /**
     * 登录用户名
     */
    private String userName;

    /**
     * 登录密码（密文存储）
     */
    private String password;

    /**
     * 邮箱
     */
    private String email;

    /**
     * 账号状态
     */
    private UserStatusEnum status;
}
