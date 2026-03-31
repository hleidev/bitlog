package top.harrylei.bitlog.gateway.filter;

import io.jsonwebtoken.Claims;
import io.jsonwebtoken.JwtException;
import io.jsonwebtoken.Jwts;
import io.jsonwebtoken.security.Keys;
import jakarta.annotation.PostConstruct;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.cloud.gateway.filter.GatewayFilterChain;
import org.springframework.cloud.gateway.filter.GlobalFilter;
import org.springframework.core.Ordered;
import org.springframework.core.io.buffer.DataBuffer;
import org.springframework.data.redis.core.ReactiveStringRedisTemplate;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.http.server.reactive.ServerHttpRequest;
import org.springframework.http.server.reactive.ServerHttpResponse;
import org.springframework.stereotype.Component;
import org.springframework.util.StringUtils;
import org.springframework.web.server.ServerWebExchange;
import reactor.core.publisher.Mono;
import top.harrylei.bitlog.gateway.config.AuthProperties;
import top.harrylei.bitlog.gateway.config.JwtProperties;

import javax.crypto.SecretKey;
import java.nio.charset.StandardCharsets;

/**
 * JWT 全局过滤器
 * <p>
 * 职责：验证 JWT → 校验 Redis 一致性 → 向下游注入 X-User-Id / X-User-Role header
 * 下游服务无需再解析 JWT，直接读取 header 即可。
 *
 * @author harry
 * @since 0.0.1
 */
@Slf4j
@Component
@RequiredArgsConstructor
public class JwtGlobalFilter implements GlobalFilter, Ordered {

    private static final String BEARER_PREFIX = "Bearer ";
    private static final String REDIS_KEY_PREFIX = "byte_logs:user:token:";

    private final JwtProperties jwtProperties;
    private final AuthProperties authProperties;
    private final ReactiveStringRedisTemplate redisTemplate;

    private SecretKey secretKey;

    @PostConstruct
    public void init() {
        secretKey = Keys.hmacShaKeyFor(jwtProperties.getSecret().getBytes());
    }

    @Override
    public int getOrder() {
        // 数字越小优先级越高，-100 保证在路由之前执行
        return -100;
    }

    @Override
    public Mono<Void> filter(ServerWebExchange exchange, GatewayFilterChain chain) {
        String path = exchange.getRequest().getPath().value();

        // 白名单路径直接放行
        if (isWhitelisted(path)) {
            return chain.filter(exchange);
        }

        String token = extractToken(exchange.getRequest());
        if (!StringUtils.hasText(token)) {
            return unauthorized(exchange);
        }

        Claims claims = parseClaims(token);
        if (claims == null) {
            return unauthorized(exchange);
        }

        String userId = claims.getSubject();
        String redisKey = REDIS_KEY_PREFIX + userId;

        // 异步校验 Redis 中的 token 一致性
        return redisTemplate.opsForValue().get(redisKey)
                .flatMap(storedToken -> {
                    if (!token.equals(storedToken)) {
                        log.debug("Redis token 不匹配，userId={}", userId);
                        return unauthorized(exchange);
                    }
                    // 向下游注入用户信息 header
                    Object roleObj = claims.get("role");
                    String role = roleObj != null ? String.valueOf(roleObj) : "";
                    ServerHttpRequest mutatedRequest = exchange.getRequest().mutate()
                            .header("X-User-Id", userId)
                            .header("X-User-Role", role)
                            .build();
                    return chain.filter(exchange.mutate().request(mutatedRequest).build());
                })
                .switchIfEmpty(Mono.defer(() -> {
                    log.debug("Redis 中无 token，userId={}", userId);
                    return unauthorized(exchange);
                }));
    }

    private boolean isWhitelisted(String path) {
        return authProperties.getWhitelist().stream().anyMatch(path::startsWith);
    }

    private String extractToken(ServerHttpRequest request) {
        String header = request.getHeaders().getFirst(HttpHeaders.AUTHORIZATION);
        if (StringUtils.hasText(header) && header.startsWith(BEARER_PREFIX)) {
            return header.substring(BEARER_PREFIX.length()).trim();
        }
        return null;
    }

    private Claims parseClaims(String token) {
        try {
            return Jwts.parserBuilder()
                    .setSigningKey(secretKey)
                    .build()
                    .parseClaimsJws(token)
                    .getBody();
        } catch (JwtException e) {
            log.debug("JWT 解析失败: {}", e.getMessage());
            return null;
        }
    }

    private Mono<Void> unauthorized(ServerWebExchange exchange) {
        ServerHttpResponse response = exchange.getResponse();
        response.setStatusCode(HttpStatus.UNAUTHORIZED);
        response.getHeaders().setContentType(MediaType.APPLICATION_JSON);
        String body = "{\"code\":40001,\"message\":\"Token 无效或已过期\",\"data\":null}";
        DataBuffer buffer = response.bufferFactory()
                .wrap(body.getBytes(StandardCharsets.UTF_8));
        return response.writeWith(Mono.just(buffer));
    }
}
