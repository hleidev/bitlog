package top.harrylei.bitlog.common.converter;

import org.springframework.core.convert.TypeDescriptor;
import org.springframework.core.convert.converter.GenericConverter;
import org.springframework.lang.NonNull;
import org.springframework.lang.Nullable;
import org.springframework.stereotype.Component;

import java.lang.reflect.Method;
import java.util.Collections;
import java.util.Optional;
import java.util.Set;
import java.util.concurrent.ConcurrentHashMap;

/**
 * 通用枚举转换器：将 query parameter 中的数字字符串转换为带 fromCode(Integer) 方法的枚举。 覆盖所有业务枚举，无需逐枚举手写 Converter。
 *
 * @author Harry
 * @since 2026-05-17
 */
@Component
@SuppressWarnings({"unchecked", "rawtypes"})
public class StringToCodeEnumConverter implements GenericConverter {

    private final ConcurrentHashMap<Class<?>, Optional<Method>> methodCache = new ConcurrentHashMap<>();

    @Override
    public Set<ConvertiblePair> getConvertibleTypes() {
        return Collections.singleton(new ConvertiblePair(String.class, Enum.class));
    }

    @Override
    @Nullable
    public Object convert(@Nullable Object source, @NonNull TypeDescriptor sourceType,
        @NonNull TypeDescriptor targetType) {
        if (source == null) {
            return null;
        }
        String str = ((String)source).trim();
        if (str.isBlank()) {
            return null;
        }
        Class<?> enumType = targetType.getType();
        Optional<Method> methodOpt = methodCache.computeIfAbsent(enumType, this::resolveFromCode);
        try {
            if (methodOpt.isPresent()) {
                Object result = methodOpt.get().invoke(null, Integer.valueOf(str));
                if (result == null) {
                    throw new IllegalArgumentException("枚举 " + enumType.getSimpleName() + " 中不存在编码: " + str);
                }
                return result;
            }
            return Enum.valueOf((Class<Enum>)enumType, str);
        } catch (NumberFormatException e) {
            throw new IllegalArgumentException("枚举 " + enumType.getSimpleName() + " 的值不合法: " + str);
        } catch (IllegalArgumentException e) {
            throw e;
        } catch (Exception e) {
            throw new IllegalArgumentException("无法将 '" + source + "' 转换为 " + enumType.getSimpleName(), e);
        }
    }

    private Optional<Method> resolveFromCode(Class<?> type) {
        try {
            return Optional.of(type.getMethod("fromCode", Integer.class));
        } catch (NoSuchMethodException e) {
            // 枚举无 fromCode 方法，转换时回退到 Enum.valueOf 名称匹配
            return Optional.empty();
        }
    }
}