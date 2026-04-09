package top.harrylei.bitlog.file.model;

import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import lombok.Data;
import lombok.experimental.Accessors;

/**
 * 预签名上传请求
 *
 * @author harry
 * @since 0.0.1
 */
@Data
@Accessors(chain = true)
@Schema(description = "预签名上传请求")
public class PresignRequest {

    @NotNull(message = "场景不能为空")
    @Schema(description = "上传场景")
    private UploadScene scene;

    @NotBlank(message = "文件名不能为空")
    @Schema(description = "原始文件名（用于提取扩展名）")
    private String fileName;

    @NotBlank(message = "文件类型不能为空")
    @Schema(description = "MIME 类型，如 image/jpeg")
    private String contentType;
}
