package top.harrylei.bitlog.api.model.user.vo;

import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Data;
import lombok.EqualsAndHashCode;
import lombok.experimental.Accessors;

/**
 * 用户详情展示对象
 *
 * @author Harry
 * @since 2026-03-17
 */
@Data
@Accessors(chain = true)
@EqualsAndHashCode(callSuper = true)
@Schema(description = "用户详情展示对象")
public class UserDetailVO extends UserListVO {

    @Schema(description = "职位", example = "Java 开发工程师")
    private String position;

    @Schema(description = "公司", example = "Bytelogs Inc.")
    private String company;

    @Schema(description = "个人简介", example = "专注于后端开发与系统架构。")
    private String profile;
}
