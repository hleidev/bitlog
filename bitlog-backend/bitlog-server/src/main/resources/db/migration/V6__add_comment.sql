CREATE TABLE `comment`
(
    `id`               bigint unsigned NOT NULL AUTO_INCREMENT COMMENT '主键ID',
    `article_id`       bigint unsigned NOT NULL COMMENT '文章ID（逻辑关联 article.id）',
    `user_id`          bigint unsigned NOT NULL COMMENT '评论者用户ID（逻辑关联 user_account.id）',
    `root_id`          bigint unsigned NOT NULL DEFAULT 0 COMMENT '根评论ID，0 表示自身即根评论',
    `parent_id`        bigint unsigned NOT NULL DEFAULT 0 COMMENT '父评论ID，0 表示一级评论',
    `reply_to_user_id` bigint unsigned NOT NULL DEFAULT 0 COMMENT '被回复者用户ID，0 表示直接回复根评论',
    `content`          varchar(1000)   NOT NULL COMMENT '评论内容（纯文本）',
    `status`           tinyint         NOT NULL DEFAULT 1 COMMENT '状态：1-正常，2-已隐藏',
    `deleted`          tinyint         NOT NULL DEFAULT 0 COMMENT '删除标记：0-正常，1-已删除',
    `create_time`      timestamp       NOT NULL DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
    `update_time`      timestamp       NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP COMMENT '更新时间',
    PRIMARY KEY (`id`),
    KEY `idx_article_root_time` (`article_id`, `root_id`, `create_time`),
    KEY `idx_root_time` (`root_id`, `create_time`),
    KEY `idx_user_id` (`user_id`)
) ENGINE = InnoDB
  DEFAULT CHARSET = utf8mb4
  COLLATE = utf8mb4_unicode_ci COMMENT ='评论表';
