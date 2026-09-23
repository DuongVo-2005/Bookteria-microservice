-- idea-spec BA OPS-03: khoá tài khoản có thời hạn - locked_until NULL nghĩa là khoá vĩnh viễn
-- (PERMANENT) hoặc chưa từng bị khoá; lock_reason lưu lý do Admin nhập lúc khoá.
ALTER TABLE `user`
    ADD COLUMN `locked_until` DATETIME(6) NULL,
    ADD COLUMN `lock_reason` VARCHAR(255) NULL;
