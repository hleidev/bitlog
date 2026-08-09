-- 角色是授权属性，原先落在 user_info 这张注销时会被 anonymize 的资料表上。
-- 迁到账号主体表后，登录与刷新令牌不必再为取角色多查一次 user_info。
ALTER TABLE `user_account`
    ADD COLUMN `user_role` tinyint NOT NULL DEFAULT 0 COMMENT '角色：0-普通用户，1-管理员' AFTER `status`;

UPDATE `user_account` a
    JOIN `user_info` b ON b.user_id = a.id
SET a.user_role = b.user_role;

ALTER TABLE `user_info`
    DROP COLUMN `user_role`;
