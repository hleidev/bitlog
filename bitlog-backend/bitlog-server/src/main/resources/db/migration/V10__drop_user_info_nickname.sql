-- 登录标识已迁移到邮箱，username 卸下登录职责后即为唯一且可修改的展示名，
-- nickname 与其职责重叠，统一由 username 承担
ALTER TABLE `user_info` DROP COLUMN `nickname`;
