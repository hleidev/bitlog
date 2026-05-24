package top.harrylei.bitlog.ai.port;

import top.harrylei.bitlog.ai.model.AiFeature;

/**
 * AI 模型路由器，根据功能类型将请求分发到对应的模型
 * <p>
 * 当前由 {@code ConfigAiModelRouter} 从配置文件中解析； 后续落表后可替换为 DB 驱动实现，业务层无需感知。
 *
 * @author Harry
 * @since 2026-05-24
 */
public interface AiModelRouter {

    /**
     * 按功能路由并执行单轮对话
     *
     * @param feature AI 功能标识（携带能力类型）
     * @param systemPrompt 系统提示词
     * @param userMessage 用户消息
     * @return 模型返回的文本
     */
    String chat(AiFeature feature, String systemPrompt, String userMessage);
}
