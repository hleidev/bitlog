package top.harrylei.bitlog.link.support;

import org.springframework.util.StringUtils;

import java.net.URI;
import java.net.URISyntaxException;
import java.util.Locale;
import java.util.Set;

/**
 * 站点地址归一化
 * <p>
 * 唯一索引比的是字符串，https://a.com 与 https://a.com/ 在库里是两条， 因此写入前必须先归一化，否则同一个站点能重复申请。
 * </p>
 *
 * @author Harry
 * @since 2026-08-15
 */
public final class SiteUrlNormalizer {

    /** 只放行 http/https：javascript: 之类进了 href 就是公开页上的执行入口 */
    private static final Set<String> ALLOWED_SCHEMES = Set.of("http", "https");

    private SiteUrlNormalizer() {}

    /**
     * 判断是否为协议合法的绝对地址
     */
    public static boolean isValid(String url) {
        return parse(url) != null;
    }

    /**
     * 归一化站点地址：协议与主机名转小写，去掉末尾斜杠
     * <p>
     * 不去 www 前缀——带与不带确实可能是两个站点，替用户决定会误合并。
     * </p>
     *
     * @param url 原始地址
     * @return 归一化后的地址，地址非法时返回 null
     */
    public static String normalize(String url) {
        URI uri = parse(url);
        if (uri == null) {
            return null;
        }

        StringBuilder sb = new StringBuilder();
        sb.append(uri.getScheme().toLowerCase(Locale.ROOT)).append("://");
        sb.append(uri.getHost().toLowerCase(Locale.ROOT));
        // 显式写出的默认端口要去掉，否则 https://a.com 与 https://a.com:443 会被当成两个站
        int port = uri.getPort();
        if (port != -1 && port != defaultPortOf(uri.getScheme())) {
            sb.append(':').append(port);
        }

        String query = uri.getRawQuery();
        String fragment = uri.getRawFragment();
        String path = uri.getRawPath();

        if (StringUtils.hasText(path) && !"/".equals(path)) {
            sb.append(path.endsWith("/") ? path.substring(0, path.length() - 1) : path);
        } else if (StringUtils.hasText(query) || StringUtils.hasText(fragment)) {
            // 根路径平时省略，但后面还挂着查询或片段时不能省：
            // https://a.com/#/home 去掉这个斜杠就成了 https://a.com#/home
            sb.append('/');
        }

        if (StringUtils.hasText(query)) {
            sb.append('?').append(query);
        }
        // 片段要保留：SPA 站点的首页可能就挂在 hash 路由上，去掉会指向错误的页面
        if (StringUtils.hasText(fragment)) {
            sb.append('#').append(fragment);
        }
        return sb.toString();
    }

    private static int defaultPortOf(String scheme) {
        return "https".equalsIgnoreCase(scheme) ? 443 : 80;
    }

    private static URI parse(String url) {
        if (!StringUtils.hasText(url)) {
            return null;
        }
        try {
            URI uri = new URI(url.trim());
            if (uri.getScheme() == null || uri.getHost() == null) {
                return null;
            }
            return ALLOWED_SCHEMES.contains(uri.getScheme().toLowerCase(Locale.ROOT)) ? uri : null;
        } catch (URISyntaxException e) {
            return null;
        }
    }
}
