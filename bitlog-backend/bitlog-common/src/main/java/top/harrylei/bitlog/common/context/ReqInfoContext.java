package top.harrylei.bitlog.common.context;

import com.alibaba.ttl.TransmittableThreadLocal;
import lombok.Data;
import lombok.experimental.Accessors;
import lombok.extern.slf4j.Slf4j;

import java.util.ArrayList;
import java.util.List;

/**
 * 请求上下文管理类
 * <p>
 * 跨线程传播依赖 {@code AsyncConfig} 对执行器的 TtlExecutors 包装，去掉包装则退化为 InheritableThreadLocal 并串号。
 *
 * @author Harry
 * @since 2026-03-17
 */
@Slf4j
public class ReqInfoContext {

    public static final String ROLE_ADMIN = "ROLE_ADMIN";

    private static final TransmittableThreadLocal<ReqInfo> CONTENT = new TransmittableThreadLocal<>();

    public static void setContext(ReqInfo reqInfo) {
        CONTENT.set(reqInfo);
    }

    /**
     * 获取当前线程的请求上下文，不存在时自动创建空上下文
     */
    public static ReqInfo getContext() {
        ReqInfo reqInfo = CONTENT.get();
        if (reqInfo == null) {
            log.debug("尝试获取未初始化的请求上下文，自动创建空上下文");
            reqInfo = new ReqInfo();
            CONTENT.set(reqInfo);
        }
        return reqInfo;
    }

    /**
     * 清除当前线程的请求上下文，应在请求处理完成后调用，避免内存泄漏
     */
    public static void clear() {
        CONTENT.remove();
    }

    @Data
    @Accessors(chain = true)
    public static class ReqInfo {

        /**
         * 用户 ID，已登录用户不为空
         */
        private Long userId;

        /**
         * 权限串列表，取值形如 ROLE_ADMIN
         */
        private List<String> authorities = new ArrayList<>();

        /**
         * 客户端 IP
         */
        private String clientIp;

        /**
         * 请求域名
         */
        private String host;

        /**
         * 请求路径
         */
        private String path;

        /**
         * 请求来源
         */
        private String referer;

        /**
         * 用户代理
         */
        private String userAgent;

        /**
         * 判断当前用户是否为管理员
         */
        public boolean isAdmin() {
            return authorities != null && authorities.contains(ROLE_ADMIN);
        }

        /**
         * 判断当前用户是否已登录
         */
        public boolean isLoggedIn() {
            return userId != null && userId > 0;
        }
    }
}
