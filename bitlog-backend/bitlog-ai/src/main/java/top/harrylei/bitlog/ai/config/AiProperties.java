package top.harrylei.bitlog.ai.config;

import jakarta.validation.Valid;
import lombok.Data;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.validation.annotation.Validated;
import top.harrylei.bitlog.common.config.EnvInjected;

import java.time.Duration;
import java.util.HashMap;
import java.util.Map;

/**
 * AI 配置属性
 * <p>
 * providers：供应商注册表，key 为供应商名称（如 qwen、deepseek）<br>
 * capabilities：能力类型路由表，key 为 {@link top.harrylei.bitlog.ai.model.AiCapability#getKey()}
 *
 * @author Harry
 * @since 2026-05-24
 */
@Data
@Validated
@ConfigurationProperties(prefix = "bitlog.ai")
public class AiProperties {

    @Valid
    private Map<String, ProviderConfig> providers = new HashMap<>();
    private Map<String, CapabilityConfig> capabilities = new HashMap<>();

    @Data
    public static class ProviderConfig {
        private String baseUrl;
        @EnvInjected
        private String apiKey;
        private Duration timeout = Duration.ofSeconds(30);
    }

    @Data
    public static class CapabilityConfig {
        /** 对应 providers 中的 key */
        private String provider;
        private String model;
        /** 透传给模型请求体的额外参数，如 enable_thinking */
        private Map<String, Object> extraParams = new HashMap<>();
    }
}
