-- 去掉 DEFAULT '' 是有意的：唯一列留空串默认值，只会让漏写字段的插入撞 uk_username
ALTER TABLE `user_account`
    MODIFY COLUMN `username` varchar(16) NOT NULL COMMENT '用户名，唯一且可修改的公开展示名';
