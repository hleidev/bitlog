package top.harrylei.community.user.repository.entity;

import com.baomidou.mybatisplus.annotation.TableName;
import lombok.Data;
import lombok.EqualsAndHashCode;
import lombok.experimental.Accessors;
import top.harrylei.community.api.enums.common.DeleteStatusEnum;
import top.harrylei.community.api.enums.user.UserRoleEnum;
import top.harrylei.community.common.model.BaseDO;

import java.io.Serial;

/**
 * 用户个人信息实体
 *
 * @author harry
 * @since 0.0.1
 */
@Data
@EqualsAndHashCode(callSuper = true)
@TableName("user_info")
@Accessors(chain = true)
public class UserInfoDO extends BaseDO {

    @Serial
    private static final long serialVersionUID = 1L;

    /**
     * 用户 ID
     */
    private Long userId;

    /**
     * 用户名
     */
    private String userName;

    /**
     * 头像
     */
    private String avatar;

    /**
     * 职位
     */
    private String position;

    /**
     * 公司
     */
    private String company;

    /**
     * 个人简介
     */
    private String profile;

    /**
     * 扩展字段
     */
    private String extend;

    /**
     * 删除标记
     */
    private DeleteStatusEnum deleted;

    /**
     * 用户角色：0-普通用户，1-超级管理员
     */
    private UserRoleEnum userRole;

    /**
     * 邮箱
     */
    private String email;
}
