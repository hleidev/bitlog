package top.harrylei.bitlog.common.config;

import java.util.Arrays;
import org.springframework.core.convert.converter.Converter;
import org.springframework.core.convert.converter.ConverterFactory;
import org.springframework.lang.NonNull;
import top.harrylei.bitlog.common.enums.CodedEnum;

/**
 * 把查询参数里的整数 code 转成对应的枚举常量
 * <p>
 * 响应用 code 表示枚举，请求也应当接受 code；枚举名同样保留，避免既有按名传参的调用方失效
 * </p>
 *
 * @author Harry
 * @since 2026-09-09
 */
public class CodedEnumConverterFactory implements ConverterFactory<String, CodedEnum> {

    @Override
    @NonNull
    public <T extends CodedEnum> Converter<String, T> getConverter(@NonNull Class<T> targetType) {
        return source -> {
            String value = source.trim();
            if (value.isEmpty()) {
                return null;
            }
            T[] constants = targetType.getEnumConstants();
            if (constants == null) {
                throw new IllegalArgumentException("目标类型不是枚举: " + targetType.getName());
            }
            return Arrays.stream(constants)
                    .filter(constant -> value.equals(String.valueOf(constant.getCode()))
                            || value.equals(((Enum<?>) constant).name()))
                    .findFirst()
                    .orElseThrow(() ->
                            new IllegalArgumentException("无效的枚举取值: " + targetType.getSimpleName() + " = " + value));
        };
    }
}
