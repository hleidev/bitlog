package top.harrylei.bitlog.common.converter;

import org.springframework.core.convert.converter.Converter;
import org.springframework.stereotype.Component;
import top.harrylei.bitlog.common.util.DateUtil;

import java.time.LocalDateTime;

/**
 * 全局参数转换器：将前端传入的字符串转换为 LocalDateTime，支持多种日期格式。
 *
 * @author harry
 * @since 0.0.1
 */
@Component
public class StringToLocalDateTimeConverter implements Converter<String, LocalDateTime> {

    @Override
    public LocalDateTime convert(String source) {
        if (source.trim().isEmpty()) {
            return null;
        }
        try {
            return DateUtil.parseDateTime(source);
        } catch (Exception e) {
            throw new IllegalArgumentException("日期格式不正确，应为 yyyy-MM-dd HH:mm:ss", e);
        }
    }
}
