package top.harrylei.community.api.enums.common;

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
 * 状态枚举
 *
 * @author harry
 * @since 0.0.1
 */
@Getter
@AllArgsConstructor
public enum StatusEnum {

    DISABLED(0, "禁用"),
    ENABLED(1, "启用");

    @EnumValue
    private final Integer code;
    private final String label;

    private static final Map<Integer, StatusEnum> CODE_MAP =
            Arrays.stream(values()).collect(Collectors.toMap(StatusEnum::getCode, Function.identity()));

    @JsonValue
    public Integer getCode() {
        return code;
    }

    @JsonCreator
    public static StatusEnum fromCode(Integer code) {
        return code == null ? null : CODE_MAP.get(code);
    }

    public boolean isEnabled() {
        return this == ENABLED;
    }
}
