package top.harrylei.bitlog.file.service.impl;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.scheduling.annotation.Async;
import org.springframework.stereotype.Service;
import org.springframework.util.StringUtils;
import org.springframework.web.multipart.MultipartFile;
import software.amazon.awssdk.core.sync.RequestBody;
import software.amazon.awssdk.services.s3.S3Client;
import software.amazon.awssdk.services.s3.model.PutObjectRequest;
import top.harrylei.bitlog.common.enums.DeleteStatusEnum;
import top.harrylei.bitlog.common.enums.ResultCode;
import top.harrylei.bitlog.common.util.FileUrlHelper;
import top.harrylei.bitlog.file.config.StorageProperties;
import top.harrylei.bitlog.file.model.UploadScene;
import top.harrylei.bitlog.file.model.UploadVO;
import top.harrylei.bitlog.file.repository.dao.ImageRecordDAO;
import top.harrylei.bitlog.file.repository.entity.ImageRecordDO;
import top.harrylei.bitlog.file.service.FileService;
import top.harrylei.bitlog.file.util.ImageProcessor;

import java.io.IOException;
import java.time.LocalDateTime;
import java.util.Collection;
import java.util.List;
import java.util.UUID;

/**
 * 文件服务实现
 *
 * @author Harry
 * @since 2026-04-09
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class FileServiceImpl implements FileService {

    private final S3Client s3Client;
    private final StorageProperties props;
    private final FileUrlHelper fileUrlHelper;
    private final ImageRecordDAO imageRecordDAO;

    @Override
    public UploadVO upload(Long userId, UploadScene scene, MultipartFile file) {
        if (file == null || file.isEmpty()) {
            ResultCode.INVALID_PARAMETER.throwException("文件不能为空");
        }

        String contentType = file.getContentType() != null ? file.getContentType().toLowerCase() : null;
        if (!scene.isAllowedType(contentType)) {
            ResultCode.FILE_TYPE_NOT_ALLOWED.throwException(contentType);
        }

        if (scene.isSizeExceeded(file.getSize())) {
            ResultCode.FILE_SIZE_EXCEEDED.throwException("最大允许 " + scene.readableMaxSize());
        }

        String ext = StringUtils.getFilenameExtension(file.getOriginalFilename());
        if (ext == null || ext.isBlank()) {
            ResultCode.INVALID_PARAMETER.throwException("文件名缺少扩展名");
        }

        byte[] fileBytes;
        try {
            fileBytes = ImageProcessor.stripExif(file.getBytes(), contentType);
        } catch (IOException e) {
            log.error("图片处理失败 scene={} userId={}", scene, userId, e);
            ResultCode.INTERNAL_ERROR.throwException("图片处理失败");
            throw new IllegalStateException("unreachable");
        }

        if (scene.isSizeExceeded(fileBytes.length)) {
            ResultCode.FILE_SIZE_EXCEEDED.throwException("最大允许 " + scene.readableMaxSize());
        }

        LocalDateTime now = LocalDateTime.now();
        String key = String.format("bitlog/%s/%d/%d/%02d/%s.%s", scene, userId, now.getYear(), now.getMonthValue(),
            UUID.randomUUID(), ext);

        try {
            s3Client.putObject(PutObjectRequest.builder().bucket(props.getBucket()).key(key).contentType(contentType)
                .contentLength((long)fileBytes.length).build(), RequestBody.fromBytes(fileBytes));
        } catch (Exception e) {
            log.error("文件上传失败 scene={} userId={} key={}", scene, userId, key, e);
            ResultCode.INTERNAL_ERROR.throwException("文件上传失败");
        }

        log.info("文件上传成功 scene={} userId={} key={}", scene, userId, key);

        if (scene == UploadScene.article_content) {
            try {
                recordUpload(userId, key);
            } catch (Exception e) {
                log.warn("内容图片记录失败 key={}", key, e);
            }
        }

        return new UploadVO().setFileKey(key).setFileUrl(fileUrlHelper.buildUrl(key));
    }

    @Override
    public void recordUpload(Long userId, String fileKey) {
        imageRecordDAO
            .save(new ImageRecordDO().setUserId(userId).setFileKey(fileKey).setDeleted(DeleteStatusEnum.NOT_DELETED));
    }

    @Override
    public List<String> getOldUndeletedContentKeys(LocalDateTime before) {
        return imageRecordDAO.getKeysOlderThan(before);
    }

    @Override
    public void markDeleted(Collection<String> fileKeys) {
        imageRecordDAO.markDeleted(fileKeys);
    }

    @Async
    @Override
    public void delete(String key) {
        if (!StringUtils.hasText(key)) {
            return;
        }
        try {
            s3Client.deleteObject(builder -> builder.bucket(props.getBucket()).key(key).build());
            log.info("文件删除成功 key={}", key);
        } catch (Exception e) {
            log.warn("文件删除失败 key={}", key, e);
        }
    }
}
