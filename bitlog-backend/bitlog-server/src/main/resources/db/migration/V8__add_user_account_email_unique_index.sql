UPDATE `user_account` SET `email` = NULL WHERE `email` = '';

ALTER TABLE `user_account`
    ADD UNIQUE KEY `uk_email` (`email`);
