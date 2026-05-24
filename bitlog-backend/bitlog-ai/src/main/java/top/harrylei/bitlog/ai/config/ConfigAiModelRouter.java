package top.harrylei.bitlog.ai.config;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import top.harrylei.bitlog.ai.client.OpenAiCompatibleClient;
import top.harrylei.bitlog.ai.model.AiFeature;
import top.harrylei.bitlog.ai.port.AiChatPort;
import top.harrylei.bitlog.ai.port.AiModelRouter;
import top.harrylei.bitlog.common.enums.ResultCode;

import java.util.concurrent.ConcurrentHashMap;

/**
 * 基于配置文件的 AI 模型路由器实现，后续落表后替换为 DB 驱动实现即可，业务层无需改动。
 *
 * @author Harry
 * @since 2026-05-24
 */
@Slf4j
@RequiredArgsConstructor
public class ConfigAiModelRouter implements AiModelRouter {

    private final AiProperties properties;
    /** 按能力类型 key 缓存客户端实例 */
    private final ConcurrentHashMap<String, AiChatPort> clientCache = new ConcurrentHashMap<>();

    @Override
    public String chat(AiFeature feature, String systemPrompt, String userMessage) {
        String capabilityKey = feature.getCapability().getKey();

        AiProperties.CapabilityConfig capConfig = properties.getCapabilities().get(capabilityKey);
        if (capConfig == null) {
            throw ResultCode.AI_NOT_CONFIGURED.toException("能力类型未配置: " + capabilityKey);
        }

        AiProperties.ProviderConfig providerConfig = properties.getProviders().get(capConfig.getProvider());
        if (providerConfig == null) {
            throw ResultCode.AI_NOT_CONFIGURED.toException("Provider 未配置: " + capConfig.getProvider());
        }

        AiChatPort client = clientCache.computeIfAbsent(capabilityKey,
            k -> new OpenAiCompatibleClient(providerConfig.getBaseUrl(), providerConfig.getApiKey(),
                capConfig.getModel(), providerConfig.getTimeout(), capConfig.getExtraParams()));

        log.info("AI 路由 feature={} capability={} provider={} model={}", feature.name(), capabilityKey,
            capConfig.getProvider(), capConfig.getModel());
        return client.chat(systemPrompt, userMessage);
    }
}
