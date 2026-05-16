CREATE TABLE `tag`
(
    `id`            bigint unsigned NOT NULL AUTO_INCREMENT COMMENT '主键ID',
    `name`          varchar(64)     NOT NULL DEFAULT '' COMMENT '标签名称',
    `article_count` int unsigned    NOT NULL DEFAULT 0 COMMENT '关联文章数',
    `create_time`   timestamp       NOT NULL DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
    `update_time`   timestamp       NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP COMMENT '更新时间',
    PRIMARY KEY (`id`),
    UNIQUE KEY `uk_name` (`name`)
) ENGINE = InnoDB
  DEFAULT CHARSET = utf8mb4
  COLLATE = utf8mb4_unicode_ci COMMENT ='标签表';

CREATE TABLE `category`
(
    `id`            bigint unsigned NOT NULL AUTO_INCREMENT COMMENT '主键ID',
    `name`          varchar(64)     NOT NULL DEFAULT '' COMMENT '分类名称',
    `article_count` int unsigned    NOT NULL DEFAULT 0 COMMENT '关联文章数',
    `create_time`   timestamp       NOT NULL DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
    `update_time`   timestamp       NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP COMMENT '更新时间',
    PRIMARY KEY (`id`),
    UNIQUE KEY `uk_name` (`name`)
) ENGINE = InnoDB
  DEFAULT CHARSET = utf8mb4
  COLLATE = utf8mb4_unicode_ci COMMENT ='文章分类表';

CREATE TABLE `article`
(
    `id`                   bigint unsigned NOT NULL AUTO_INCREMENT COMMENT '主键ID',
    `user_id`              bigint unsigned NOT NULL COMMENT '作者用户ID',
    `category_id`          bigint unsigned          DEFAULT NULL COMMENT '分类ID（逻辑关联 category.id）',
    `latest_version_id`    bigint unsigned          DEFAULT NULL COMMENT '最新版本ID（逻辑关联 article_version.id）',
    `published_version_id` bigint unsigned          DEFAULT NULL COMMENT '已发布版本ID，NULL 表示未发布',
    `publish_time`         timestamp                NULL DEFAULT NULL COMMENT '首次发布时间，NULL 表示从未发布',
    `cover`                varchar(512)    NOT NULL DEFAULT '' COMMENT '封面图地址',
    `summary`              varchar(512)    NOT NULL DEFAULT '' COMMENT '文章摘要',
    `topping`              tinyint         NOT NULL DEFAULT 0 COMMENT '置顶：0-否，1-是',
    `version_count`        int unsigned    NOT NULL DEFAULT 0 COMMENT '版本总数',
    `deleted`              tinyint         NOT NULL DEFAULT 0 COMMENT '是否删除：0-未删除，1-已删除',
    `create_time`          timestamp       NOT NULL DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
    `update_time`          timestamp       NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP COMMENT '更新时间',
    PRIMARY KEY (`id`),
    KEY `idx_user_id` (`user_id`),
    KEY `idx_category_id` (`category_id`),
    KEY `idx_published_version_id` (`published_version_id`)
) ENGINE = InnoDB
  DEFAULT CHARSET = utf8mb4
  COLLATE = utf8mb4_unicode_ci COMMENT ='文章主表';

CREATE TABLE `article_version`
(
    `id`          bigint unsigned NOT NULL AUTO_INCREMENT COMMENT '主键ID',
    `article_id`  bigint unsigned NOT NULL COMMENT '文章ID（逻辑关联 article.id）',
    `version`     int unsigned    NOT NULL DEFAULT 1 COMMENT '版本号',
    `title`       varchar(200)    NOT NULL DEFAULT '' COMMENT '文章标题',
    `content`     longtext        NOT NULL COMMENT '文章内容',
    `create_time` timestamp       NOT NULL DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
    PRIMARY KEY (`id`),
    UNIQUE KEY `uk_article_version` (`article_id`, `version`),
    KEY `idx_article_id` (`article_id`)
) ENGINE = InnoDB
  DEFAULT CHARSET = utf8mb4
  COLLATE = utf8mb4_unicode_ci COMMENT ='文章版本表（只追加）';

CREATE TABLE `article_tag`
(
    `id`          bigint unsigned NOT NULL AUTO_INCREMENT COMMENT '主键ID',
    `article_id`  bigint unsigned NOT NULL COMMENT '文章ID',
    `tag_id`      bigint unsigned NOT NULL COMMENT '标签ID',
    `create_time` timestamp       NOT NULL DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
    PRIMARY KEY (`id`),
    UNIQUE KEY `uk_article_tag` (`article_id`, `tag_id`),
    KEY `idx_tag_id` (`tag_id`)
) ENGINE = InnoDB
  DEFAULT CHARSET = utf8mb4
  COLLATE = utf8mb4_unicode_ci COMMENT ='文章标签关联表';

CREATE TABLE `article_statistics`
(
    `id`            bigint unsigned NOT NULL AUTO_INCREMENT COMMENT '主键ID',
    `article_id`    bigint unsigned NOT NULL COMMENT '文章ID',
    `read_count`    int unsigned    NOT NULL DEFAULT 0 COMMENT '阅读数',
    `comment_count` int unsigned    NOT NULL DEFAULT 0 COMMENT '评论数',
    `create_time`   timestamp       NOT NULL DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
    `update_time`   timestamp       NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP COMMENT '更新时间',
    PRIMARY KEY (`id`),
    UNIQUE KEY `uk_article_id` (`article_id`)
) ENGINE = InnoDB
  DEFAULT CHARSET = utf8mb4
  COLLATE = utf8mb4_unicode_ci COMMENT ='文章统计表';
