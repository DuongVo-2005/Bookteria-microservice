package com.devteria.identity.entity;

import java.time.Instant;
import java.util.Set;

import jakarta.persistence.*;

import lombok.*;
import lombok.experimental.FieldDefaults;

@Getter
@Setter
@Builder
@NoArgsConstructor
@AllArgsConstructor
@FieldDefaults(level = AccessLevel.PRIVATE)
@Entity
public class User {
    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    String id;

    @Column(name = "username", unique = true, columnDefinition = "VARCHAR(255) COLLATE utf8mb4_unicode_ci")
    String username;

    String password;

    @Column(name = "email", unique = true, columnDefinition = "VARCHAR(255) COLLATE utf8mb4_unicode_ci")
    String email;

    @Column(name = "email_verified", nullable = false, columnDefinition = "boolean default false")
    boolean emailVerified;

    // Google "sub" (subject) — định danh bất biến của Google account, KHÔNG dùng
    // email để định danh Google vì email có thể đổi hoặc chưa verified.
    // null nghĩa là user chưa từng link Google.
    @Column(name = "google_sub", unique = true, columnDefinition = "VARCHAR(255)")
    String googleSub;

    // idea-spec Phase 4 - "11.1 Session Revocation" — Admin khoá tài khoản, chặn ở
    // AuthenticationService.verifyToken() (gateway gọi mỗi request qua introspect()).
    @Column(name = "locked", nullable = false, columnDefinition = "boolean default false")
    boolean locked;

    // idea-spec BA OPS-03: khoá có thời hạn - null nghĩa là khoá vĩnh viễn (PERMANENT) hoặc user
    // chưa từng bị khoá lần nào. UserAutoUnlockJob quét cột này để tự khôi phục khi hết hạn.
    @Column(name = "locked_until")
    Instant lockedUntil;

    @Column(name = "lock_reason")
    String lockReason;

    // idea-spec BA v2 §2.2: "Soft Delete & Anonymize" - khác locked (tạm thời/có thời hạn/có thể
    // appeal), deactivated là tự Admin đóng tài khoản + ẩn profile công khai, không có lockedUntil
    // đi kèm (không tự khôi phục). verifyToken() chặn y hệt user bị locked.
    @Column(name = "deactivated", nullable = false, columnDefinition = "boolean default false")
    boolean deactivated;

    @ManyToMany
    Set<Role> roles;

    // idea-spec BA v2 P1-01: quyền gán trực tiếp cho user, ngoài permission suy ra từ Role — vd
    // cấp riêng 1 quyền catalog cho 1 USER thường mà không cần nâng hẳn lên LIBRARIAN.
    @ManyToMany
    Set<Permission> permissions;
}
