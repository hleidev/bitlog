package top.harrylei.bitlog.api.model.user.dto;

import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Data;
import lombok.EqualsAndHashCode;
import lombok.experimental.Accessors;
import top.harrylei.bitlog.common.enums.DeleteStatusEnum;
import top.harrylei.bitlog.api.enums.user.UserRoleEnum;
import top.harrylei.bitlog.common.model.BaseDTO;

/**
 * 用户基础实体对象
 *
 * @author Harry
 * @since 2026-03-17
 */
@EqualsAndHashCode(callSuper = true)
@Data
@Schema(description = "用户基础实体对象")
@Accessors(chain = true)
public class UserDTO extends BaseDTO {

    @Schema(description = "用户id", requiredMode = Schema.RequiredMode.REQUIRED)
    private Long userId;

    @Schema(description = "用户名", requiredMode = Schema.RequiredMode.REQUIRED)
    private String username;

    @Schema(description = "角色", example = "ADMIN|NORMAL")
    private UserRoleEnum userRole;

    @Schema(description = "用户头像")
    private String avatar;

    @Schema(description = "用户简介")
    private String profile;

    @Schema(description = "个人职位")
    private String position;

    @Schema(description = "公司")
    private String company;

    @Schema(hidden = true)
    private String extend;

    @Schema(hidden = true, description = "用户是否被删除")
    private DeleteStatusEnum deleted;

    @Schema(description = "用户邮箱", example = "bitlog@gmail.com")
    private String email;
}
