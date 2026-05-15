package top.harrylei.bitlog.file.controller;

import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.multipart.MultipartFile;
import top.harrylei.bitlog.common.context.ReqInfoContext;
import top.harrylei.bitlog.common.model.Result;
import top.harrylei.bitlog.common.security.RequiresLogin;
import top.harrylei.bitlog.file.model.UploadScene;
import top.harrylei.bitlog.file.model.UploadVO;
import top.harrylei.bitlog.file.service.FileService;

/**
 * 文件上传接口
 *
 * @author harry
 * @since 0.0.1
 */
@Tag(name = "文件上传接口")
@RestController
@RequestMapping("/api/v1/file")
@RequiredArgsConstructor
public class FileController {

    private final FileService fileService;

    @RequiresLogin
    @Operation(summary = "上传文件")
    @PostMapping("/upload")
    public Result<UploadVO> upload(
            @RequestParam UploadScene scene,
            @RequestParam MultipartFile file) {
        Long userId = ReqInfoContext.getContext().getUserId();
        return Result.success(fileService.upload(userId, scene, file));
    }
}
