package top.harrylei.bitlog.comment.model.enums;

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
 * 评论状态枚举
 *
 * @author Harry
 * @since 2026-07-28
 */
@Getter
@AllArgsConstructor
public enum CommentStatusEnum {

    NORMAL(1, "正常"), HIDDEN(2, "已隐藏");

    @EnumValue
    private final Integer code;
    private final String label;

    private static final Map<Integer, CommentStatusEnum> CODE_MAP =
        Arrays.stream(values()).collect(Collectors.toMap(CommentStatusEnum::getCode, Function.identity()));

    @JsonValue
    public Integer getCode() {
        return code;
    }

    @JsonCreator
    public static CommentStatusEnum fromCode(Integer code) {
        return code == null ? null : CODE_MAP.get(code);
    }
}
