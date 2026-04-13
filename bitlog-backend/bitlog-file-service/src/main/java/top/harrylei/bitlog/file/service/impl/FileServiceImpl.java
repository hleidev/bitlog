package top.harrylei.bitlog.file.service.impl;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.util.StringUtils;
import org.springframework.web.multipart.MultipartFile;
import software.amazon.awssdk.core.sync.RequestBody;
import software.amazon.awssdk.services.s3.S3Client;
import software.amazon.awssdk.services.s3.model.PutObjectRequest;
import top.harrylei.bitlog.common.enums.ResultCode;
import top.harrylei.bitlog.file.config.StorageProperties;
import top.harrylei.bitlog.file.model.UploadScene;
import top.harrylei.bitlog.file.model.UploadVO;
import top.harrylei.bitlog.file.service.FileService;

import java.io.IOException;
import java.time.LocalDateTime;
import java.util.UUID;

/**
 * 文件服务实现
 *
 * @author harry
 * @since 0.0.1
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class FileServiceImpl implements FileService {

    private final S3Client s3Client;
    private final StorageProperties props;

    @Override
    public UploadVO upload(Long userId, UploadScene scene, MultipartFile file) {
        if (file == null || file.isEmpty()) {
            ResultCode.INVALID_PARAMETER.throwException("文件不能为空");
        }

        String ext = StringUtils.getFilenameExtension(file.getOriginalFilename());
        if (ext == null || ext.isBlank()) {
            ResultCode.INVALID_PARAMETER.throwException("文件名缺少扩展名");
        }

        LocalDateTime now = LocalDateTime.now();
        String key = String.format("bitlog/%s/%d/%d/%02d/%s.%s",
                scene, userId,
                now.getYear(), now.getMonthValue(),
                UUID.randomUUID(), ext);

        try {
            s3Client.putObject(
                    PutObjectRequest.builder()
                            .bucket(props.getBucket())
                            .key(key)
                            .contentType(file.getContentType())
                            .contentLength(file.getSize())
                            .build(),
                    RequestBody.fromInputStream(file.getInputStream(), file.getSize())
            );
        } catch (IOException e) {
            log.error("文件上传失败 scene={} userId={} key={}", scene, userId, key, e);
            ResultCode.INTERNAL_ERROR.throwException("文件上传失败");
        }

        String fileUrl = props.getPublicUrl() + "/" + key;
        log.info("文件上传成功 scene={} userId={} key={}", scene, userId, key);

        return new UploadVO().setFileUrl(fileUrl);
    }
}
