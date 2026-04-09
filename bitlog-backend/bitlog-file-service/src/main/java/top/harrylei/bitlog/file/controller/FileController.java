package top.harrylei.bitlog.file.controller;

import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;
import top.harrylei.bitlog.common.context.ReqInfoContext;
import top.harrylei.bitlog.common.model.Result;
import top.harrylei.bitlog.common.security.RequiresLogin;
import top.harrylei.bitlog.file.model.PresignRequest;
import top.harrylei.bitlog.file.model.PresignVO;
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
    @Operation(summary = "获取预签名上传地址")
    @PostMapping("/presign")
    public Result<PresignVO> presign(@Valid @RequestBody PresignRequest req) {
        Long userId = ReqInfoContext.getContext().getUserId();
        return Result.success(fileService.presign(userId, req));
    }
}
