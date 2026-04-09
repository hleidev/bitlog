package top.harrylei.bitlog.file.service.impl;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.util.StringUtils;
import software.amazon.awssdk.services.s3.model.PutObjectRequest;
import software.amazon.awssdk.services.s3.presigner.S3Presigner;
import software.amazon.awssdk.services.s3.presigner.model.PresignedPutObjectRequest;
import top.harrylei.bitlog.common.enums.ResultCode;
import top.harrylei.bitlog.file.config.StorageProperties;
import top.harrylei.bitlog.file.model.PresignRequest;
import top.harrylei.bitlog.file.model.PresignVO;
import top.harrylei.bitlog.file.service.FileService;

import java.time.Duration;
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

    private final S3Presigner presigner;
    private final StorageProperties props;

    @Override
    public PresignVO presign(Long userId, PresignRequest req) {
        String ext = StringUtils.getFilenameExtension(req.getFileName());
        if (ext == null || ext.isBlank()) {
            ResultCode.INVALID_PARAMETER.throwException("文件名缺少扩展名");
        }

        LocalDateTime now = LocalDateTime.now();
        String key = String.format("bitlog/%s/%d/%d/%02d/%s.%s",
                req.getScene(), userId,
                now.getYear(), now.getMonthValue(),
                UUID.randomUUID(), ext);

        PutObjectRequest putReq = PutObjectRequest.builder()
                .bucket(props.getBucket())
                .key(key)
                .contentType(req.getContentType())
                .build();

        PresignedPutObjectRequest presigned = presigner.presignPutObject(r -> r
                .signatureDuration(Duration.ofMinutes(props.getPresignExpireMinutes()))
                .putObjectRequest(putReq)
                .build());

        String fileUrl = props.getPublicUrl() + "/" + key;
        log.info("生成预签名 URL scene={} userId={} key={}", req.getScene(), userId, key);

        return new PresignVO()
                .setUploadUrl(presigned.url().toString())
                .setFileUrl(fileUrl);
    }
}
