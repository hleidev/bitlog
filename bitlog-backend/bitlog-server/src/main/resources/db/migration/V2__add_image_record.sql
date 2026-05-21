CREATE TABLE `image_record`
(
    `id`          bigint unsigned NOT NULL AUTO_INCREMENT COMMENT '主键ID',
    `user_id`     bigint unsigned NOT NULL COMMENT '上传用户ID',
    `file_key`    varchar(512)    NOT NULL DEFAULT '' COMMENT '文件存储Key',
    `deleted`     tinyint         NOT NULL DEFAULT 0 COMMENT '删除标记：0-正常，1-已删除',
    `create_time` timestamp       NOT NULL DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
    `update_time` timestamp       NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP COMMENT '更新时间',
    PRIMARY KEY (`id`),
    UNIQUE KEY `uk_file_key` (`file_key`),
    KEY `idx_user_id_deleted` (`user_id`, `deleted`)
) ENGINE = InnoDB
  DEFAULT CHARSET = utf8mb4
  COLLATE = utf8mb4_unicode_ci
  COMMENT = '文章内容图片上传记录';
