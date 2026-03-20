package top.harrylei.community.api.model.user.query;

import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Data;
import lombok.EqualsAndHashCode;
import lombok.experimental.Accessors;
import org.springframework.format.annotation.DateTimeFormat;
import top.harrylei.community.api.enums.common.DeleteStatusEnum;
import top.harrylei.community.common.model.BasePage;

import java.time.LocalDateTime;
import java.util.Map;

/**
 * 用户列表查询参数
 *
 * @author harry
 * @since 0.0.1
 */
@Data
@EqualsAndHashCode(callSuper = true)
@Schema(description = "用户列表查询参数")
@Accessors(chain = true)
public class UserPageQuery extends BasePage {

    private static final Map<String, String> FIELD_MAPPING = Map.of(
            "userId", "id",
            "userName", "user_name",
            "email", "email",
            "status", "status",
            "role", "user_role",
            "createTime", "create_time",
            "updateTime", "update_time",
            "deleted", "deleted"
    );

    @Schema(description = "用户名")
    private String userName;

    @Schema(description = "用户状态")
    private Integer status;

    @Schema(description = "是否删除")
    private DeleteStatusEnum deleted;

    @Schema(description = "注册起始时间", example = "2025-01-01 00:00:00")
    @DateTimeFormat(pattern = "yyyy-MM-dd HH:mm:ss")
    private LocalDateTime startTime;

    @Schema(description = "注册结束时间", example = "2025-12-31 23:59:59")
    @DateTimeFormat(pattern = "yyyy-MM-dd HH:mm:ss")
    private LocalDateTime endTime;

    @Override
    public Map<String, String> getFieldMapping() {
        return FIELD_MAPPING;
    }
}
