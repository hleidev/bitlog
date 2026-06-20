package top.harrylei.bitlog.article.component;

import jakarta.annotation.PostConstruct;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.client.SimpleClientHttpRequestFactory;
import org.springframework.scheduling.annotation.Async;
import org.springframework.stereotype.Component;
import org.springframework.util.StringUtils;
import org.springframework.web.client.RestClient;

import java.time.Duration;

/**
 * Cloudflare Pages 部署钩子客户端：文章发布/更新/删除/下架后异步触发静态站点重新构建
 * <p>
 * fire-and-forget：异步执行，不阻塞文章写接口；未配置 {@code CLOUDFLARE_DEPLOY_HOOK_URL} 时（如本地开发） 自动跳过；调用失败仅记录 warn 日志，不影响主流程、不回滚事务。
 *
 * @author Harry
 * @since 2026-06-20
 */
@Slf4j
@Component
public class DeployHookService {

    private static final Duration CONNECT_TIMEOUT = Duration.ofSeconds(5);
    private static final Duration READ_TIMEOUT = Duration.ofSeconds(10);

    @Value("${cloudflare.deploy-hook.url:}")
    private String deployHookUrl;

    private RestClient restClient;

    @PostConstruct
    private void initRestClient() {
        SimpleClientHttpRequestFactory factory = new SimpleClientHttpRequestFactory();
        factory.setConnectTimeout(CONNECT_TIMEOUT);
        factory.setReadTimeout(READ_TIMEOUT);
        this.restClient = RestClient.builder().requestFactory(factory).build();
    }

    /**
     * 异步触发 Cloudflare Pages 重新部署
     */
    @Async
    public void triggerDeploy() {
        if (!StringUtils.hasText(deployHookUrl)) {
            return;
        }
        try {
            restClient.post().uri(deployHookUrl).retrieve().toBodilessEntity();
            log.info("Cloudflare 部署钩子已触发");
        } catch (Exception e) {
            log.warn("Cloudflare 部署钩子调用失败: {}", e.getMessage());
        }
    }
}
