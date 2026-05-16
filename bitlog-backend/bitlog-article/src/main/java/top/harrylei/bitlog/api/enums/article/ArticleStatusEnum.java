package top.harrylei.bitlog.api.enums.article;

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
 * 文章状态枚举
 *
 * @author Harry
 * @since 2026-04-09
 */
@Getter
@AllArgsConstructor
public enum ArticleStatusEnum {

    DRAFT(0, "草稿"), PUBLISHED(1, "已发布");

    @EnumValue
    private final Integer code;
    private final String label;

    private static final Map<Integer, ArticleStatusEnum> CODE_MAP = Arrays.stream(values())
            .collect(Collectors.toMap(ArticleStatusEnum::getCode, Function.identity()));

    @JsonValue
    public Integer getCode() {
        return code;
    }

    @JsonCreator
    public static ArticleStatusEnum fromCode(Integer code) {
        return code == null ? null : CODE_MAP.get(code);
    }
}
