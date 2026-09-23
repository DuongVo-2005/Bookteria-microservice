-- idea-spec BA v2 P1-02: role LIBRARIAN + permissions Catalog cho Thủ thư/biên tập viên nội dung.
INSERT INTO `permission` (`name`, `description`) VALUES
    ('book:create', 'Create new books in the catalog'),
    ('book:update', 'Update existing book details'),
    ('book:delete', 'Delete books from the catalog'),
    ('book:import', 'Import books from Google Books or batch import'),
    ('reading:import_content', 'Upload and parse ePub/PDF chapter content'),
    ('review:lock', 'Lock/unlock review writing for a book'),
    ('author:manage', 'Manage author master data'),
    ('category:manage', 'Manage category master data'),
    ('publisher:manage', 'Manage publisher master data');

INSERT INTO `role` (`name`, `description`) VALUES ('LIBRARIAN', 'Book catalog manager');

INSERT INTO `role_permissions` (`role_name`, `permissions_name`) VALUES
    ('LIBRARIAN', 'book:create'),
    ('LIBRARIAN', 'book:update'),
    ('LIBRARIAN', 'book:delete'),
    ('LIBRARIAN', 'book:import'),
    ('LIBRARIAN', 'reading:import_content'),
    ('LIBRARIAN', 'review:lock'),
    ('LIBRARIAN', 'author:manage'),
    ('LIBRARIAN', 'category:manage'),
    ('LIBRARIAN', 'publisher:manage');

-- idea-spec BA v2 P1-01: cho phép Admin tuỳ chỉnh danh sách Permission trực tiếp cho 1 user, ngoài
-- permission suy ra từ Role (vd cấp riêng 1 quyền catalog cho 1 USER thường mà không cần nâng hẳn
-- lên LIBRARIAN). Cùng shape join table với user_roles/role_permissions đã có.
CREATE TABLE `user_permissions` (
    `user_id` VARCHAR(255) NOT NULL,
    `permissions_name` VARCHAR(255) NOT NULL,
    PRIMARY KEY (`user_id`, `permissions_name`),
    CONSTRAINT `fk_user_permissions_user` FOREIGN KEY (`user_id`) REFERENCES `user` (`id`),
    CONSTRAINT `fk_user_permissions_permission` FOREIGN KEY (`permissions_name`) REFERENCES `permission` (`name`)
);
