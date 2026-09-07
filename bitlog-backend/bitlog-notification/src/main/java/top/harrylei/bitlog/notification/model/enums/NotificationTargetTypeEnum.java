package top.harrylei.bitlog.notification.model.enums;

import com.baomidou.mybatisplus.annotation.EnumValue;
import com.fasterxml.jackson.annotation.JsonCreator;
import com.fasterxml.jackson.annotation.JsonValue;
import java.util.Arrays;
import java.util.Map;
import java.util.function.Function;
import java.util.stream.Collectors;
import lombok.AllArgsConstructor;
import lombok.Getter;

/**
 * 通知目标对象类型枚举
 *
 * @author Harry
 * @since 2026-09-06
 */
@Getter
@AllArgsConstructor
public enum NotificationTargetTypeEnum {
    NONE(0, "无"),
    COMMENT(1, "评论"),
    ARTICLE(2, "文章"),
    FRIEND_LINK(3, "友链");

    @EnumValue
    private final Integer code;

    private final String label;

    private static final Map<Integer, NotificationTargetTypeEnum> CODE_MAP =
            Arrays.stream(values()).collect(Collectors.toMap(NotificationTargetTypeEnum::getCode, Function.identity()));

    @JsonValue
    public Integer getCode() {
        return code;
    }

    @JsonCreator
    public static NotificationTargetTypeEnum fromCode(Integer code) {
        return code == null ? null : CODE_MAP.get(code);
    }
}
