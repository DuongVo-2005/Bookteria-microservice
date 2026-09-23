-- idea-spec BA v2 §2.2: "Soft Delete & Anonymize" - khác locked (tạm thời), deactivated là Admin
-- tự đóng tài khoản vĩnh viễn, kèm anonymize profile ở profile-service.
ALTER TABLE `user` ADD COLUMN `deactivated` BIT(1) NOT NULL DEFAULT b'0';
