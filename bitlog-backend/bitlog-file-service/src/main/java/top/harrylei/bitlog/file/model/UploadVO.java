package top.harrylei.bitlog.file.model;

import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Data;
import lombok.experimental.Accessors;

/**
 * 文件上传响应
 *
 * @author harry
 * @since 0.0.1
 */
@Data
@Accessors(chain = true)
@Schema(description = "文件上传响应")
public class UploadVO {

    @Schema(description = "文件访问地址")
    private String fileUrl;
}
