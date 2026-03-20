package top.harrylei.community.api.model.user.query;

import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Data;
import lombok.EqualsAndHashCode;
import top.harrylei.community.common.model.BasePage;

import java.util.Map;

/**
 * 用户关注查询参数
 *
 * @author harry
 * @since 0.0.1
 */
@EqualsAndHashCode(callSuper = true)
@Data
@Schema(description = "用户关注查询参数")
public class UserFollowPageQuery extends BasePage {

    @Schema(description = "关注者用户ID", example = "1")
    private Long userId;

    @Schema(description = "被关注者用户ID", example = "2")
    private Long followUserId;

    @Schema(description = "用户名关键词", example = "张三")
    private String userName;

    private static final Map<String, String> FIELD_MAPPING = Map.of(
            "followTime", "follow_time",
            "createTime", "follow_time",
            "userName", "user_name",
            "userId", "user_id"
    );

    @Override
    public Map<String, String> getFieldMapping() {
        return FIELD_MAPPING;
    }
}
