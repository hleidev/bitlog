ALTER TABLE `user_account`
    ADD COLUMN `email_verified` tinyint NOT NULL DEFAULT 0 COMMENT '邮箱是否已验证：0-未验证，1-已验证' AFTER `email`;

ALTER TABLE `user_account`
    MODIFY COLUMN `email` varchar(128) DEFAULT NULL COMMENT '登录邮箱，统一小写存储';

ALTER TABLE `user_account`
    MODIFY COLUMN `password` char(60) DEFAULT NULL COMMENT '密码，BCrypt 加密；第三方登录用户为空';

CREATE TABLE `user_identity`
(
    `id`               bigint unsigned NOT NULL AUTO_INCREMENT COMMENT '主键ID',
    `user_id`          bigint unsigned NOT NULL COMMENT '用户ID（逻辑关联 user_account.id）',
    `provider`         varchar(32)     NOT NULL COMMENT '第三方平台标识：google',
    `provider_user_id` varchar(128)    NOT NULL COMMENT '第三方平台用户唯一ID（Google 为 sub）',
    `create_time`      timestamp       NOT NULL DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
    `update_time`      timestamp       NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP COMMENT '更新时间',
    PRIMARY KEY (`id`),
    UNIQUE KEY `uk_provider_uid` (`provider`, `provider_user_id`),
    KEY `idx_user_id` (`user_id`)
) ENGINE = InnoDB
  DEFAULT CHARSET = utf8mb4
  COLLATE = utf8mb4_general_ci COMMENT ='第三方身份关联表';
