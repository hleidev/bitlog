package top.harrylei.bitlog.ai.port;

/**
 * AI 对话能力抽象接口，上层业务依赖此接口，不依赖具体实现
 *
 * @author Harry
 * @since 2026-05-24
 */
public interface AiChatPort {

    /**
     * 单轮对话
     *
     * @param systemPrompt 系统提示词
     * @param userMessage 用户消息
     * @return 模型返回的文本
     */
    String chat(String systemPrompt, String userMessage);
}
