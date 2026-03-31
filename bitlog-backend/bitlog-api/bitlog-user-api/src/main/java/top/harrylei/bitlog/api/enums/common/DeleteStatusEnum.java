package top.harrylei.bitlog.api.enums.common;

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
 * 删除标识枚举
 *
 * @author harry
 * @since 0.0.1
 */
@Getter
@AllArgsConstructor
public enum DeleteStatusEnum {
    NOT_DELETED(0, "未删除"),
    DELETED(1, "已删除");

    @EnumValue
    private final Integer code;
    private final String label;

    private static final Map<Integer, DeleteStatusEnum> CODE_MAP =
            Arrays.stream(values()).collect(Collectors.toMap(DeleteStatusEnum::getCode, Function.identity()));

    @JsonValue
    public Integer getCode() {
        return code;
    }

    @JsonCreator
    public static DeleteStatusEnum fromCode(Integer code) {
        return code == null ? null : CODE_MAP.get(code);
    }
}
