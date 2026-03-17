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
 * 用户状态枚举
 *
 * @author harry
 * @since 0.0.1
 */
@Getter
@AllArgsConstructor
public enum UserStatusEnum {

    DISABLED(0, "禁用"),
    ENABLED(1, "启用");

    @EnumValue
    private final int code;
    private final String label;

    private static final Map<Integer, UserStatusEnum> CODE_MAP =
            Arrays.stream(values()).collect(Collectors.toMap(UserStatusEnum::getCode, Function.identity()));

    @JsonValue
    public Integer getCode() {
        return code;
    }

    @JsonCreator
    public static UserStatusEnum fromCode(Integer code) {
        return code == null ? null : CODE_MAP.get(code);
    }
}
