package top.harrylei.bitlog.api.enums.link;

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
 * 友链状态枚举
 *
 * @author Harry
 * @since 2026-08-14
 */
@Getter
@AllArgsConstructor
public enum FriendLinkStatusEnum {

    PENDING(0, "待审核"), APPROVED(1, "已通过"), REJECTED(2, "已拒绝");

    @EnumValue
    private final Integer code;
    private final String label;

    private static final Map<Integer, FriendLinkStatusEnum> CODE_MAP =
        Arrays.stream(values()).collect(Collectors.toMap(FriendLinkStatusEnum::getCode, Function.identity()));

    @JsonValue
    public Integer getCode() {
        return code;
    }

    @JsonCreator
    public static FriendLinkStatusEnum fromCode(Integer code) {
        return code == null ? null : CODE_MAP.get(code);
    }
}
