-- idea-spec Phase 4 - "11.1 Session Revocation": Admin khoá tài khoản -> user.locked=true,
-- AuthenticationService.introspect() (gateway gọi mỗi request) tự chặn token của user đã khoá.
ALTER TABLE `user` ADD COLUMN `locked` BIT(1) NOT NULL DEFAULT b'0';
