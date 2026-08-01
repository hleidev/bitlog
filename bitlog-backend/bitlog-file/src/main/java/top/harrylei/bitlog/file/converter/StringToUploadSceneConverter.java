package top.harrylei.bitlog.file.converter;

import org.springframework.core.convert.converter.Converter;
import org.springframework.stereotype.Component;
import top.harrylei.bitlog.common.enums.ResultCode;
import top.harrylei.bitlog.file.model.UploadScene;

/**
 * 请求参数到上传场景的转换器，按 code 而非枚举常量名匹配
 *
 * @author Harry
 * @since 2026-08-01
 */
@Component
public class StringToUploadSceneConverter implements Converter<String, UploadScene> {

    @Override
    public UploadScene convert(String source) {
        UploadScene scene = UploadScene.fromCode(source);
        if (scene == null) {
            ResultCode.INVALID_PARAMETER.throwException("不支持的上传场景：" + source);
        }
        return scene;
    }
}
