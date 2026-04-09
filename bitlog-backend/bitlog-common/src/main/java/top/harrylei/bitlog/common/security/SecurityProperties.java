package top.harrylei.bitlog.common.security;

import lombok.Data;
import org.springframework.boot.context.properties.ConfigurationProperties;

import java.util.ArrayList;
import java.util.List;

/**
 * 安全配置属性
 * <p>
 * 各服务通过 security.additional-whitelist 追加自己的公开路径，
 * 公共白名单（内部接口、Actuator、Swagger）已在 SecurityConfig 中预置。
 * </p>
 *
 * @author harry
 * @since 0.0.1
 */
@Data
@ConfigurationProperties(prefix = "security")
public class SecurityProperties {

    /**
     * 各服务追加的额外白名单路径
     */
    private List<String> additionalWhitelist = new ArrayList<>();
}
