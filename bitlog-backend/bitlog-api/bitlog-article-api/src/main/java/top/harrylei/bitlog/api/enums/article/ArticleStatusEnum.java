package top.harrylei.bitlog.api.enums.article;

import com.baomidou.mybatisplus.annotation.EnumValue;
import com.fasterxml.jackson.annotation.JsonValue;
import lombok.AllArgsConstructor;
import lombok.Getter;

/**
 * 文章状态枚举
 *
 * @author harry
 * @since 0.0.1
 */
@Getter
@AllArgsConstructor
public enum ArticleStatusEnum {

    DRAFT(0, "草稿"),
    PUBLISHED(1, "已发布");

    @EnumValue
    private final Integer code;
    @JsonValue
    private final String desc;
}
