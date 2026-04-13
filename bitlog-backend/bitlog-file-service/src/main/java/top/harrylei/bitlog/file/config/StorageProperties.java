package top.harrylei.bitlog.file.config;

import lombok.Data;
import org.springframework.boot.context.properties.ConfigurationProperties;

/**
 * 对象存储配置属性
 *
 * @author harry
 * @since 0.0.1
 */
@Data
@ConfigurationProperties(prefix = "storage")
public class StorageProperties {

    /** MinIO/S3 endpoint，容器内访问 */
    private String endpoint;

    /** 访问密钥 */
    private String accessKey;

    /** 秘密密钥 */
    private String secretKey;

    /** 存储桶名称 */
    private String bucket;

    /** 对外暴露的文件 URL 前缀（经 Nginx 代理后的地址） */
    private String publicUrl;

}
