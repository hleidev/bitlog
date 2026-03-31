package top.harrylei.bitlog.common.enums;

/**
 * 响应码接口，各服务自定义错误码枚举实现此接口
 *
 * @author harry
 * @since 0.0.1
 */
public interface IResultCode {

    /**
     * 状态码
     */
    int getCode();

    /**
     * 描述信息
     */
    String getMessage();
}
