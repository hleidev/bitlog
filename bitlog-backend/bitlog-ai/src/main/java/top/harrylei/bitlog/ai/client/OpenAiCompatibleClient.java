package top.harrylei.bitlog.ai.client;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.http.client.SimpleClientHttpRequestFactory;
import org.springframework.web.client.RestClient;
import org.springframework.web.client.RestClientException;
import top.harrylei.bitlog.ai.port.AiChatPort;
import top.harrylei.bitlog.common.enums.ResultCode;

import java.time.Duration;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * OpenAI 兼容接口客户端，适配通义千问、DeepSeek 等主流国内模型
 *
 * @author Harry
 * @since 2026-05-24
 */
@Slf4j
public class OpenAiCompatibleClient implements AiChatPort {

    private static final ObjectMapper OBJECT_MAPPER = new ObjectMapper();
    private static final Duration CONNECT_TIMEOUT = Duration.ofSeconds(10);
    private static final String FIELD_CONTENT = "content";

    private final String model;
    private final Map<String, Object> extraParams;
    private final RestClient restClient;

    public OpenAiCompatibleClient(String baseUrl, String apiKey, String model, Duration readTimeout,
        Map<String, Object> extraParams) {
        this.model = model;
        this.extraParams = extraParams;

        SimpleClientHttpRequestFactory factory = new SimpleClientHttpRequestFactory();
        factory.setConnectTimeout(CONNECT_TIMEOUT);
        factory.setReadTimeout(readTimeout);

        this.restClient = RestClient.builder().baseUrl(baseUrl).requestFactory(factory)
            .defaultHeader(HttpHeaders.AUTHORIZATION, "Bearer " + apiKey)
            .defaultHeader(HttpHeaders.CONTENT_TYPE, MediaType.APPLICATION_JSON_VALUE).build();
    }

    @Override
    public String chat(String systemPrompt, String userMessage) {
        Map<String, Object> body = new HashMap<>(extraParams);
        body.put("model", model);
        body.put("messages", List.of(Map.of("role", "system", FIELD_CONTENT, systemPrompt),
            Map.of("role", "user", FIELD_CONTENT, userMessage)));

        String responseBody;
        try {
            responseBody = restClient.post().uri("/chat/completions").body(body).retrieve().body(String.class);
        } catch (RestClientException e) {
            log.error("AI 接口请求失败 model={}: {}", model, e.getMessage());
            throw ResultCode.AI_SERVICE_ERROR.toException();
        }

        return parseContent(responseBody);
    }

    private String parseContent(String responseBody) {
        try {
            JsonNode root = OBJECT_MAPPER.readTree(responseBody);
            JsonNode choices = root.path("choices");
            if (!choices.isArray() || choices.isEmpty()) {
                log.error("AI 响应格式异常 model={}", model);
                throw ResultCode.AI_SERVICE_ERROR.toException();
            }
            String content = choices.get(0).path("message").path(FIELD_CONTENT).asText("");
            if (content.isBlank()) {
                log.error("AI 返回内容为空 model={}", model);
                throw ResultCode.AI_SERVICE_ERROR.toException();
            }
            // 过滤 Qwen3 thinking 块（<think>...</think>）
            return content.replaceAll("(?s)<think>.*?</think>", "").strip();
        } catch (JsonProcessingException e) {
            log.error("AI 响应解析失败 model={}: {}", model, e.getMessage());
            throw ResultCode.AI_SERVICE_ERROR.toException();
        }
    }
}
