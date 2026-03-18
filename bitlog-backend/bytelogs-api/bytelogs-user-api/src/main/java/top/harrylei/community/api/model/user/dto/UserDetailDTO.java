package top.harrylei.community.api.model.user.dto;

import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Data;
import lombok.EqualsAndHashCode;
import lombok.experimental.Accessors;
import top.harrylei.community.api.enums.common.DeleteStatusEnum;
import top.harrylei.community.api.enums.user.UserRoleEnum;
import top.harrylei.community.common.model.BaseDTO;

/**
 * 用户完整信息 DTO
 *
 * @author harry
 * @since 0.0.1
 */
@Data
@Schema(description = "用户完整信息DTO")
@EqualsAndHashCode(callSuper = false)
@Accessors(chain = true)
public class UserDetailDTO extends BaseDTO {

    @Schema(description = "用户ID")
    private Long userId;

    // ---------------- user_account 表字段 ----------------

    @Schema(description = "用户名")
    private String userName;

    @Schema(description = "邮箱")
    private String email;

    @Schema(description = "账号状态，0-禁用，1-启用")
    private Integer status;

    @Schema(description = "删除标记，0-未删除，1-已删除")
    private DeleteStatusEnum deleted;

    @Schema(description = "第三方账号ID")
    private String thirdAccountId;

    @Schema(description = "登录类型，0-密码登录，1-邮箱验证码登录")
    private Integer loginType;

    // ---------------- user_info 表字段 ----------------

    @Schema(description = "用户头像")
    private String avatar;

    @Schema(description = "职位")
    private String position;

    @Schema(description = "公司")
    private String company;

    @Schema(description = "个人简介")
    private String profile;

    @Schema(description = "扩展字段")
    private String extend;

    @Schema(description = "用户角色，0-普通用户，1-超级管理员")
    private UserRoleEnum userRole;
}
