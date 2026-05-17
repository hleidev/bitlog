package top.harrylei.bitlog.file.model;

import java.util.Set;

/**
 * 文件上传场景，定义各场景允许的 MIME 类型和文件大小上限
 *
 * @author Harry
 * @since 2026-04-09
 */
public enum UploadScene {

    /** 用户头像 */
    avatar(Set.of("image/jpeg", "image/png", "image/webp"), 1L * 1024 * 1024),

    /** 文章封面 */
    article_cover(Set.of("image/jpeg", "image/png", "image/webp"), 10L * 1024 * 1024),

    /** 文章正文内图片 */
    article_content(Set.of("image/jpeg", "image/png", "image/webp"), 5L * 1024 * 1024);

    private final Set<String> allowedMimeTypes;
    private final long maxSizeBytes;

    UploadScene(Set<String> allowedMimeTypes, long maxSizeBytes) {
        this.allowedMimeTypes = allowedMimeTypes;
        this.maxSizeBytes = maxSizeBytes;
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
