package top.harrylei.community.api.model.user.vo;

import com.fasterxml.jackson.annotation.JsonFormat;
import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Data;
import lombok.experimental.Accessors;
import top.harrylei.community.api.enums.common.DeleteStatusEnum;
import top.harrylei.community.api.enums.user.UserRoleEnum;

import java.time.LocalDateTime;

/**
 * 用户列表项展示对象
 *
 * @author harry
 * @since 0.0.1
 */
@Data
@Schema(description = "用户列表项展示对象")
@Accessors(chain = true)
public class UserListVO {

    @Schema(description = "用户ID", example = "123")
    private Long userId;

    @Schema(description = "用户名", example = "harry")
    private String userName;

    @Schema(description = "账号状态：0-禁用，1-启用", example = "1")
    private Integer status;

    @Schema(description = "角色", example = "ADMIN")
    private UserRoleEnum userRole;

    @Schema(description = "头像", example = "https://cdn.bytelogs.com/avatar.jpg")
    private String avatar;

    @Schema(description = "邮箱", example = "harry@bytelogs.com")
    private String email;

    @Schema(description = "是否已删除", example = "NO")
    private DeleteStatusEnum deleted;

    @Schema(description = "注册时间", example = "2023-04-01T12:00:00")
    @JsonFormat(pattern = "yyyy-MM-dd HH:mm:ss")
    private LocalDateTime createTime;

    @Schema(description = "最后更新时间", example = "2023-04-01T15:30:00")
    @JsonFormat(pattern = "yyyy-MM-dd HH:mm:ss")
    private LocalDateTime updateTime;
}
