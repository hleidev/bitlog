package top.harrylei.community.gateway.config;

import lombok.Data;
import org.springframework.boot.context.properties.ConfigurationProperties;

import java.util.List;

/**
 * 鉴权配置：白名单路径不经过 JWT 验证
 *
 * @author harry
 * @since 0.0.1
 */
@Data
@ConfigurationProperties(prefix = "gateway.auth")
public class AuthProperties {

    /** 不需要 JWT 的路径前缀列表 */
    private List<String> whitelist = List.of();
}
