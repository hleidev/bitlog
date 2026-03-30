-- 用户账号表
CREATE TABLE IF NOT EXISTS `user_account`
(
    `id`               bigint unsigned NOT NULL AUTO_INCREMENT COMMENT '主键ID',
    `third_account_id` varchar(128)    NOT NULL DEFAULT '' COMMENT '第三方账号ID',
    `user_name`        varchar(64)     NOT NULL DEFAULT '' COMMENT '用户名',
    `password`         char(60)        NOT NULL DEFAULT '' COMMENT '密码，BCrypt 加密',
    `login_type`       tinyint         NOT NULL DEFAULT 0 COMMENT '登录类型：0-密码，1-邮箱验证码',
    `email`            varchar(128)             DEFAULT '' COMMENT '邮箱',
    `status`           tinyint         NOT NULL DEFAULT 1 COMMENT '账号状态：0-禁用，1-启用',
    `deleted`          tinyint         NOT NULL DEFAULT 0 COMMENT '删除标记：0-正常，1-已删除',
    `create_time`      timestamp       NOT NULL DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
    `update_time`      timestamp       NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP COMMENT '更新时间',
    PRIMARY KEY (`id`),
    UNIQUE KEY `uk_user_name` (`user_name`)
) ENGINE = InnoDB
  DEFAULT CHARSET = utf8mb4
  COLLATE = utf8mb4_general_ci
    COMMENT = '用户账号表';

-- 用户信息表（email 只存 user_account，此表不重复存储）
CREATE TABLE IF NOT EXISTS `user_info`
(
    `id`          bigint unsigned NOT NULL AUTO_INCREMENT COMMENT '主键ID',
    `user_id`     bigint unsigned NOT NULL DEFAULT 0 COMMENT '关联 user_account.id',
    `user_name`   varchar(64)     NOT NULL DEFAULT '' COMMENT '用户名（冗余，便于查询）',
    `avatar`      varchar(256)    NOT NULL DEFAULT '' COMMENT '头像 URL',
    `position`    varchar(64)     NOT NULL DEFAULT '' COMMENT '职位',
    `company`     varchar(64)     NOT NULL DEFAULT '' COMMENT '公司',
    `profile`     varchar(500)    NOT NULL DEFAULT '' COMMENT '个人简介',
    `user_role`   tinyint         NOT NULL DEFAULT 0 COMMENT '角色：0-普通用户，1-管理员',
    `extend`      varchar(1024)   NOT NULL DEFAULT '' COMMENT '扩展信息，JSON 格式',
    `deleted`     tinyint         NOT NULL DEFAULT 0 COMMENT '删除标记：0-正常，1-已删除',
    `create_time` timestamp       NOT NULL DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
    `update_time` timestamp       NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP COMMENT '更新时间',
    PRIMARY KEY (`id`),
    UNIQUE KEY `uk_user_id` (`user_id`)
) ENGINE = InnoDB
  DEFAULT CHARSET = utf8mb4
  COLLATE = utf8mb4_general_ci
    COMMENT = '用户信息表';

