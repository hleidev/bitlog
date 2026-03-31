package top.harrylei.bitlog.common.util;

import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.DeserializationFeature;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.SerializationFeature;
import com.fasterxml.jackson.datatype.jsr310.JavaTimeModule;
import lombok.extern.slf4j.Slf4j;
import org.apache.commons.lang3.StringUtils;

import java.nio.charset.StandardCharsets;
import java.text.SimpleDateFormat;

/**
 * JSON 工具类
 *
 * @author harry
 * @since 0.0.1
 */
@Slf4j
public class JsonUtil {

    private static final ObjectMapper MAPPER = new ObjectMapper();

    static {
        MAPPER.configure(DeserializationFeature.FAIL_ON_UNKNOWN_PROPERTIES, false);
        MAPPER.registerModule(new JavaTimeModule());
        MAPPER.disable(SerializationFeature.WRITE_DATES_AS_TIMESTAMPS);
        MAPPER.setDateFormat(new SimpleDateFormat("yyyy-MM-dd HH:mm:ss"));
    }

    private JsonUtil() {
        throw new UnsupportedOperationException("工具类不允许实例化");
    }

    public static String toJson(Object obj) {
        if (obj == null) return null;
        try {
            return MAPPER.writeValueAsString(obj);
        } catch (Exception e) {
            log.error("对象转JSON失败: 对象类型={}, 错误信息={}", obj.getClass().getSimpleName(), e.getMessage(), e);
            return null;
        }
    }

    public static <T> T fromJson(String jsonStr, Class<T> clazz) {
        if (StringUtils.isBlank(jsonStr) || clazz == null) return null;
        try {
            return MAPPER.readValue(jsonStr, clazz);
        } catch (Exception e) {
            log.error("JSON转对象失败: JSON={}, 目标类型={}, 错误信息={}", jsonStr, clazz.getSimpleName(), e.getMessage(), e);
            return null;
        }
    }

    public static <T> T fromJson(String jsonStr, TypeReference<T> typeReference) {
        if (StringUtils.isBlank(jsonStr) || typeReference == null) return null;
        try {
            return MAPPER.readValue(jsonStr, typeReference);
        } catch (Exception e) {
            log.error("JSON转泛型对象失败: JSON={}, 目标类型={}, 错误信息={}", jsonStr, typeReference.getType(), e.getMessage(), e);
            return null;
        }
    }

    public static byte[] toBytes(Object obj) {
        String jsonStr = toJson(obj);
        return jsonStr == null ? null : jsonStr.getBytes(StandardCharsets.UTF_8);
    }

    public static <T> T fromBytes(byte[] bytes, Class<T> clazz) {
        if (bytes == null || bytes.length == 0 || clazz == null) return null;
        try {
            return fromJson(new String(bytes, StandardCharsets.UTF_8), clazz);
        } catch (Exception e) {
            log.error("字节数组转对象失败: 目标类型={}, 错误信息={}", clazz.getSimpleName(), e.getMessage(), e);
            return null;
        }
    }

    public static <T> T fromBytes(byte[] bytes, TypeReference<T> typeReference) {
        if (bytes == null || bytes.length == 0 || typeReference == null) return null;
        try {
            return fromJson(new String(bytes, StandardCharsets.UTF_8), typeReference);
        } catch (Exception e) {
            log.error("字节数组转泛型对象失败: 目标类型={}, 错误信息={}", typeReference.getType(), e.getMessage(), e);
            return null;
        }
    }

    public static JsonNode parseToNode(String jsonStr) {
        if (StringUtils.isBlank(jsonStr)) return null;
        try {
            return MAPPER.readTree(jsonStr);
        } catch (Exception e) {
            log.error("JSON解析为JsonNode失败: JSON={}, 错误信息={}", jsonStr, e.getMessage(), e);
            return null;
        }
    }
}
