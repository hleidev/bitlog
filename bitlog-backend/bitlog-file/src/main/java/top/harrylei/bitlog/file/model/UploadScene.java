package top.harrylei.bitlog.file.model;

import com.fasterxml.jackson.annotation.JsonCreator;
import com.fasterxml.jackson.annotation.JsonValue;
import lombok.AllArgsConstructor;
import lombok.Getter;

import java.util.Arrays;
import java.util.Map;
import java.util.Set;
import java.util.function.Function;
import java.util.stream.Collectors;

/**
 * 文件上传场景，定义各场景允许的 MIME 类型和文件大小上限
 *
 * @author Harry
 * @since 2026-04-09
 */
@Getter
@AllArgsConstructor
public enum UploadScene {

    /** 用户头像 */
    AVATAR("avatar", Set.of("image/jpeg", "image/png", "image/webp"), 1L * 1024 * 1024),

    /** 文章正文内图片 */
    ARTICLE("article", Set.of("image/jpeg", "image/png", "image/webp"), 5L * 1024 * 1024);

    /** 对外标识：请求参数取值与对象存储的一级目录都用它 */
    private final String code;
    private final Set<String> allowedMimeTypes;
    private final long maxSizeBytes;

    private static final Map<String, UploadScene> CODE_MAP =
        Arrays.stream(values()).collect(Collectors.toMap(UploadScene::getCode, Function.identity()));

    @JsonValue
    public String getCode() {
        return code;
    }

    @JsonCreator
    public static UploadScene fromCode(String code) {
        return code == null ? null : CODE_MAP.get(code);
    }

    public boolean isAllowedType(String contentType) {
        return contentType != null && allowedMimeTypes.contains(contentType);
    }

    public boolean isSizeExceeded(long sizeBytes) {
        return sizeBytes > maxSizeBytes;
    }

    public String readableMaxSize() {
        return (maxSizeBytes / (1024 * 1024)) + "MB";
    }
}
