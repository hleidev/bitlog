package top.harrylei.bitlog.notification.model.enums;

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
 * 通知类型枚举
 *
 * @author Harry
 * @since 2026-09-06
 */
@Getter
@AllArgsConstructor
public enum NotificationTypeEnum {

    COMMENT_REPLY(1, "有人回复我的评论", 50), ARTICLE_COMMENT(2, "我的文章有新评论", 20), LINK_APPLIED(3, "新友链申请", 30),
    LINK_REVIEWED(4, "友链审核结果", 40), SYSTEM(5, "系统通知", 100);

    @EnumValue
    private final Integer code;
    private final String label;

    /**
     * 同批同收件人去重时的优先级，数值越大越优先；被人直接回复比「我的文章有人评论了」更具体
     */
    private final int priority;

    private static final Map<Integer, NotificationTypeEnum> CODE_MAP =
        Arrays.stream(values()).collect(Collectors.toMap(NotificationTypeEnum::getCode, Function.identity()));

    @JsonValue
    public Integer getCode() {
        return code;
    }

    @JsonCreator
    public static NotificationTypeEnum fromCode(Integer code) {
        return code == null ? null : CODE_MAP.get(code);
    }
}
