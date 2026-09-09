package top.harrylei.bitlog.common.enums;

/**
 * 以整数 code 作为对外表示的枚举
 * <p>
 * Spring MVC 默认只按枚举名绑定查询参数，实现本接口的枚举由 CodedEnumConverterFactory 支持按 code 绑定
 * </p>
 *
 * @author Harry
 * @since 2026-09-09
 */
public interface CodedEnum {

    /**
     * 对外暴露的编码
     *
     * @return 编码
     */
    Integer getCode();
}
