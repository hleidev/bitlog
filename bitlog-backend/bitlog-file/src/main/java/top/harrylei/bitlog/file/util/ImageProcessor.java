package top.harrylei.bitlog.file.util;

import net.coobird.thumbnailator.Thumbnails;

import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.IOException;

/**
 * 图片处理工具：剥离 EXIF 元数据（含 GPS 坐标等隐私信息）
 *
 * @author Harry
 * @since 2026-05-17
 */
public final class ImageProcessor {

    private ImageProcessor() {}

    /**
     * 剥离图片 EXIF 元数据（含 GPS 坐标等隐私信息）。 WebP 跳过处理：Java ImageIO 不支持 WebP 写入，且 WebP 鲜少携带 GPS 元数据。
     */
    public static byte[] stripExif(byte[] inputBytes, String contentType) throws IOException {
        if ("image/webp".equals(contentType)) {
            return inputBytes;
        }
        String format = "image/png".equals(contentType) ? "png" : "jpg";
        ByteArrayOutputStream out = new ByteArrayOutputStream(inputBytes.length);
        Thumbnails.of(new ByteArrayInputStream(inputBytes)).scale(1.0).outputFormat(format).outputQuality(0.85)
            .toOutputStream(out);
        return out.toByteArray();
    }
}
