package top.harrylei.bitlog.file.service;

import org.springframework.web.multipart.MultipartFile;
import top.harrylei.bitlog.file.model.UploadScene;
import top.harrylei.bitlog.file.model.UploadVO;

/**
 * 文件服务接口
 *
 * @author harry
 * @since 0.0.1
 */
public interface FileService {

    /**
     * 上传文件至对象存储
     *
     * @param userId 当前用户 ID
     * @param scene  上传场景
     * @param file   上传的文件
     * @return 文件访问地址
     */
    UploadVO upload(Long userId, UploadScene scene, MultipartFile file);
}
