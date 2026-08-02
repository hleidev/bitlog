-- 第三方侧邮箱与本站登录邮箱无从属关系，用户可用 A 邮箱注册、绑定 B 邮箱的 Google，
-- 需存下来供界面展示究竟绑的是哪个第三方账号
ALTER TABLE `user_identity`
    ADD COLUMN `provider_email` varchar(128) DEFAULT NULL COMMENT '第三方平台侧邮箱，与本站登录邮箱无从属关系' AFTER `provider_user_id`;

-- 一个账号在同一平台只应绑一个第三方号。原表仅约束 (provider, provider_user_id)，
-- 同一账号可重复绑多个 Google：登录都能进，但界面无从表达当前绑的是哪一个
ALTER TABLE `user_identity`
    ADD UNIQUE KEY `uk_user_provider` (`user_id`, `provider`);
