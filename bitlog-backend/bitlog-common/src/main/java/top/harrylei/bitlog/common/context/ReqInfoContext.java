package top.harrylei.bitlog.common.context;

import com.alibaba.ttl.TransmittableThreadLocal;
import lombok.Data;
import lombok.experimental.Accessors;
import lombok.extern.slf4j.Slf4j;

import java.util.ArrayList;
import java.util.List;

/**
 * 请求上下文管理类
 * 使用 TransmittableThreadLocal 存储请求上下文信息，支持异步线程传递。
 * <p>
 * 在微服务架构下，用户信息由 Gateway 验证 JWT 后通过 Header 传入：
 * X-User-Id   → userId
 * X-User-Role → authorities
 * 各服务拦截器从 Header 读取并存入此上下文，不再持有完整的 UserInfoDTO。
 * 需要完整用户信息时，通过 Feign 调用用户服务获取。
 *
 * @author harry
 * @since 0.0.1
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
         * 用户角色列表，存储角色字符串（如 "ROLE_ADMIN"）。
         * 不使用 SimpleGrantedAuthority，避免 bitlog-common 依赖 Spring Security，
         * 防止 Servlet 栈与 WebFlux 栈（Gateway）之间的类路径冲突。
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
