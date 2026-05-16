package top.harrylei.bitlog.api.model.user.vo;

import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Data;
import top.harrylei.bitlog.api.enums.user.UserRoleEnum;

/**
 * 用户信息展示对象
 *
 * @author Harry
 * @since 2026-03-17
 */
@Data
@Schema(description = "用户信息展示对象")
public class UserVO {

    @Schema(description = "用户 ID", example = "123")
    private Long userId;

    @Schema(description = "昵称", example = "harry")
    private String nickname;

    @Schema(description = "角色", example = "ADMIN")
    private UserRoleEnum userRole;

    @Schema(description = "头像", example = "https://cdn.bitlog.top/avatar.jpg")
    private String avatar;

    @Schema(description = "用户简介", example = "热爱开源和后端开发")
    private String profile;

    @Schema(description = "职位", example = "后端工程师")
    private String position;

    @Schema(description = "公司", example = "Bytelogs Inc.")
    private String company;

    @Schema(description = "邮箱", example = "harry@bitlog.top")
    private String email;
}
