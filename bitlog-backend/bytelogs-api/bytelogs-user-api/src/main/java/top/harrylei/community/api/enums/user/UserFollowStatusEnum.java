package top.harrylei.community.api.enums.user;

import com.baomidou.mybatisplus.annotation.EnumValue;
import com.fasterxml.jackson.annotation.JsonCreator;
import com.fasterxml.jackson.annotation.JsonValue;
import lombok.AllArgsConstructor;
import lombok.Getter;

import java.util.Arrays;
import java.util.Map;
import java.util.function.Function;
import java.util.stream.Collectors;

/**
 * 用户关注状态枚举
 *
 * @author harry
 * @since 0.0.1
 */
@Getter
@AllArgsConstructor
public enum UserFollowStatusEnum {

    UNFOLLOWED(0, "未关注"),
    FOLLOWED(1, "已关注");

    @EnumValue
    private final Integer code;
    private final String label;

    private static final Map<Integer, UserFollowStatusEnum> CODE_MAP =
            Arrays.stream(values()).collect(Collectors.toMap(UserFollowStatusEnum::getCode, Function.identity()));

    @JsonValue
    public Integer getCode() {
        return code;
    }

    @JsonCreator
    public static UserFollowStatusEnum fromCode(Integer code) {
        return code == null ? null : CODE_MAP.get(code);
    }
}
