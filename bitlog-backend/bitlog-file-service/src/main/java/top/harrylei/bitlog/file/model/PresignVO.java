package top.harrylei.bitlog.file.model;

import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Data;
import lombok.experimental.Accessors;

/**
 * 预签名上传响应
 *
 * @author harry
 * @since 0.0.1
 */
@Data
@Accessors(chain = true)
@Schema(description = "预签名上传响应")
public class PresignVO {

    @Schema(description = "直传地址（PUT 方法，5 分钟有效）")
    private String uploadUrl;

    @Schema(description = "文件最终访问地址")
    private String fileUrl;
}
