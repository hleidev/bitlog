package top.harrylei.bitlog.file.service;

import top.harrylei.bitlog.file.model.PresignRequest;
import top.harrylei.bitlog.file.model.PresignVO;

/**
 * 文件服务接口
 *
 * @author harry
 * @since 0.0.1
 */
public interface FileService {

    /**
     * 生成预签名上传地址
     *
     * @param userId 当前用户 ID
     * @param req    上传请求参数
     * @return 预签名上传地址和文件访问地址
     */
    PresignVO presign(Long userId, PresignRequest req);
}
