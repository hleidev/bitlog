package top.harrylei.bitlog.file.service;

import org.springframework.web.multipart.MultipartFile;
import top.harrylei.bitlog.file.model.UploadScene;
import top.harrylei.bitlog.file.model.UploadVO;

import java.time.LocalDateTime;
import java.util.Collection;
import java.util.List;

/**
 * 文件服务接口
 *
 * @author harry
 * @since 2026-04-09
 */
public interface FileService {

    /**
     * 上传文件至对象存储
     *
     * @param userId 当前用户 ID
     * @param scene 上传场景
     * @param file 上传的文件
     * @return 文件访问地址
     */
    UploadVO upload(Long userId, UploadScene scene, MultipartFile file);

    /**
     * 删除对象存储中的文件，失败时仅记录日志，不抛出异常
     *
     * @param key 文件存储路径（如 bitlog/avatar/1/2026/04/xxx.jpeg）
     */
    void delete(String key);

    /**
     * 记录 article_content 场景的图片上传，用于 GC 跟踪
     *
     * @param userId 上传用户 ID
     * @param fileKey 文件存储 Key
     */
    void recordUpload(Long userId, String fileKey);

    /**
     * 查询早于指定时间且尚未标记删除的内容图片 Key 列表
     *
     * @param before 时间阈值
     * @return file_key 列表
     */
    List<String> getOldUndeletedContentKeys(LocalDateTime before);

    /**
     * 批量将图片记录标记为已删除
     *
     * @param fileKeys file_key 集合
     */
    void markDeleted(Collection<String> fileKeys);
}
