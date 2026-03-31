package top.harrylei.bitlog.api.enums.user;

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
 * 用户角色枚举
 *
 * @author harry
 * @since 0.0.1
 */
@Getter
@AllArgsConstructor
public enum UserRoleEnum {

    NORMAL(0, "普通用户"),
    ADMIN(1, "管理员");

    @EnumValue
    private final Integer code;
    private final String label;

    private static final Map<Integer, UserRoleEnum> CODE_MAP =
            Arrays.stream(values()).collect(Collectors.toMap(UserRoleEnum::getCode, Function.identity()));

    @JsonValue
    public Integer getCode() {
        return code;
    }

    @JsonCreator
    public static UserRoleEnum fromCode(Integer code) {
        return code == null ? null : CODE_MAP.get(code);
    }
}
