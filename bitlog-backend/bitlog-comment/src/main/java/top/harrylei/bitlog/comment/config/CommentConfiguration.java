package top.harrylei.bitlog.comment.config;

import org.springframework.boot.context.properties.EnableConfigurationProperties;
import org.springframework.context.annotation.Configuration;

/**
 * 评论域配置
 *
 * @author Harry
 * @since 2026-07-29
 */
@Configuration
@EnableConfigurationProperties(CommentProperties.class)
public class CommentConfiguration {}
