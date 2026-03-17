package top.harrylei.community.common.util;

/**
 * 数值类型通用工具类
 *
 * @author harry
 * @since 0.0.1
 */
public class NumUtil {

    private NumUtil() {
        throw new UnsupportedOperationException("工具类不允许实例化");
    }

    public static boolean nullOrZero(Long num) {
        return num == null || num == 0L;
    }

    public static boolean nullOrZero(Integer num) {
        return num == null || num == 0;
    }

    public static boolean upZero(Long num) {
        return num != null && num > 0;
    }

    public static boolean upZero(Integer num) {
        return num != null && num > 0;
    }
}
