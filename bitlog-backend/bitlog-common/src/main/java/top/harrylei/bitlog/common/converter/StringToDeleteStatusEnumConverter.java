package top.harrylei.bitlog.common.converter;

import org.springframework.core.convert.converter.Converter;
import org.springframework.stereotype.Component;
import top.harrylei.bitlog.common.enums.DeleteStatusEnum;

/**
 * 全局参数转换器：将前端传入的字符串转换为 DeleteStatusEnum。
 *
 * @author harry
 * @since 0.0.1
 */
@Component
public class StringToDeleteStatusEnumConverter implements Converter<String, DeleteStatusEnum> {

    @Override
    public DeleteStatusEnum convert(String source) {
        try {
            return DeleteStatusEnum.fromCode(Integer.valueOf(source.trim()));
        } catch (NumberFormatException e) {
            throw new IllegalArgumentException("删除状态值不合法，应为 0 或 1");
        }
    }
}
