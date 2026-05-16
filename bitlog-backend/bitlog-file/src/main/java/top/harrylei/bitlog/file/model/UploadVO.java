package top.harrylei.bitlog.file.model;

import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Data;
import lombok.experimental.Accessors;

/**
 * 文件上传响应
 *
 * @author Harry
 * 
 * @since 2026-04-09
 */
@Data
@Accessors(chain = true)
@Schema(description = "文件上传响应")
public class UploadVO {

    @Schema(description = "文件存储路径（key）")
    private String fileKey;

    @Schema(description = "文件可访问的完整 URL")
    private String fileUrl;
}
