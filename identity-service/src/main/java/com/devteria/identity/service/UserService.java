package com.devteria.identity.service;

import java.time.Instant;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.UUID;

import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.kafka.core.KafkaTemplate;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.util.CollectionUtils;
import org.springframework.util.StringUtils;

import com.devteria.event.dto.NotificationEvent;
import com.devteria.identity.constant.PredefinedRole;
import com.devteria.identity.dto.OutboxEventType;
import com.devteria.identity.dto.request.LockUserRequest;
import com.devteria.identity.dto.request.UserCreationRequest;
import com.devteria.identity.dto.request.UserPermissionUpdateRequest;
import com.devteria.identity.dto.request.UserUpdateRequest;
import com.devteria.identity.dto.response.UserResponse;
import com.devteria.identity.entity.Permission;
import com.devteria.identity.entity.Role;
import com.devteria.identity.entity.User;
import com.devteria.identity.exception.AppException;
import com.devteria.identity.exception.ErrorCode;
import com.devteria.identity.mapper.ProfileMapper;
import com.devteria.identity.mapper.UserMapper;
import com.devteria.identity.repository.PermissionRepository;
import com.devteria.identity.repository.RoleRepository;
import com.devteria.identity.repository.UserRepository;
import com.devteria.identity.repository.httpclient.ProfileClient;

import lombok.AccessLevel;
import lombok.RequiredArgsConstructor;
import lombok.experimental.FieldDefaults;
import lombok.extern.slf4j.Slf4j;

@Service
@RequiredArgsConstructor
@FieldDefaults(level = AccessLevel.PRIVATE, makeFinal = true)
@Slf4j
public class UserService {
    UserRepository userRepository;
    RoleRepository roleRepository;
    PermissionRepository permissionRepository;
    UserMapper userMapper;
    PasswordEncoder passwordEncoder;
    ProfileClient profileClient;
    ProfileMapper profileMapper;
    KafkaTemplate<String, Object> kafkaTemplate;
    OutboxEventService outboxEventService;

    @Transactional
    public UserResponse createUser(UserCreationRequest request) {
        if (userRepository.existsByUsername(request.getUsername())) throw new AppException(ErrorCode.USER_EXISTED);
        // Chặn Case 2: email đã dùng bởi 1 account khác (kể cả account tạo qua Google).
        if (userRepository.existsByEmail(request.getEmail())) throw new AppException(ErrorCode.EMAIL_EXISTED);

        User user = userMapper.toUser(request);
        user.setPassword(passwordEncoder.encode(request.getPassword()));

        HashSet<Role> roles = new HashSet<>();
        roleRepository.findById(PredefinedRole.USER_ROLE).ifPresent(roles::add);

        user.setRoles(roles);

        try {
            user = userRepository.save(user);
        } catch (DataIntegrityViolationException e) {
            // Lưới an toàn cuối cùng cho race condition: 2 request cùng pass check existsByEmail/
            // existsByUsername ở trên rồi cùng insert - DB unique constraint sẽ chặn 1 trong 2,
            // ở đây convert exception mơ hồ đó thành lỗi rõ ràng cho FE.
            throw new AppException(ErrorCode.EMAIL_EXISTED);
        }

        var profileRequest = profileMapper.toProfileCreationRequest(request);
        profileRequest.setUserId(user.getId());

        var profileResponse = profileClient.createProfile(profileRequest);

        // idea-spec BA GAP-01: ghi vào Outbox (cùng transaction JPA với việc tạo User/save ở
        // trên) thay vì publish thẳng kafkaTemplate.send() — trước đây mất welcome email luôn
        // nếu Kafka down đúng lúc đăng ký, giờ OutboxPublisher tự retry cho tới khi publish được.
        NotificationEvent notificationEvent = NotificationEvent.builder()
                .channel("EMAIL")
                .userId(user.getId())
                .recipient(request.getEmail())
                .subject("Welcome to bookteria")
                .body("Hello, " + request.getUsername())
                .build();
        outboxEventService.recordEvent(user.getId(), OutboxEventType.WELCOME_EMAIL, notificationEvent);

        return userMapper.toUserResponse(user);
    }

    public UserResponse getMyInfo() {
        var context = SecurityContextHolder.getContext();
        // JWT subject là user.getId() (xem AuthenticationService.generateToken), không phải username.
        String userId = context.getAuthentication().getName();

        User user = userRepository.findById(userId).orElseThrow(() -> new AppException(ErrorCode.USER_NOT_EXISTED));

        return userMapper.toUserResponse(user);
    }

    @PreAuthorize("hasRole('ADMIN')")
    public UserResponse updateUser(String userId, UserUpdateRequest request) {
        User user = userRepository.findById(userId).orElseThrow(() -> new AppException(ErrorCode.USER_NOT_EXISTED));

        userMapper.updateUser(user, request);

        // password/roles là optional cho update 1 phần (vd chỉ đổi tên) — trước đây gọi thẳng
        // passwordEncoder.encode(null)/roleRepository.findAllById(null) khi client không gửi 2
        // field này, ném IllegalArgumentException/InvalidDataAccessApiUsageException không bắt
        // được, lộ ra thành lỗi chung chung "Uncategorized error". Chỉ đổi khi client thực sự gửi.
        if (StringUtils.hasText(request.getPassword())) {
            user.setPassword(passwordEncoder.encode(request.getPassword()));
        }

        if (!CollectionUtils.isEmpty(request.getRoles())) {
            var roles = roleRepository.findAllById(request.getRoles());
            user.setRoles(new HashSet<>(roles));
        }

        return userMapper.toUserResponse(userRepository.save(user));
    }

    // idea-spec BA v2 P1-01: "Tùy chỉnh danh sách Quyền (Permissions) trực tiếp cho từng tài
    // khoản" - khác updateUser().roles (gán Role), đây gán thẳng Permission riêng lẻ, không đi
    // qua Role nào (vd cấp 1 quyền catalog cho 1 USER thường mà không cần nâng lên LIBRARIAN).
    // Thay thế toàn bộ danh sách, không phải thêm/bớt từng cái - cùng convention với roles.
    @PreAuthorize("hasRole('ADMIN')")
    public UserResponse updatePermissions(String userId, UserPermissionUpdateRequest request) {
        User user = userRepository.findById(userId).orElseThrow(() -> new AppException(ErrorCode.USER_NOT_EXISTED));

        List<Permission> permissions = CollectionUtils.isEmpty(request.getPermissions())
                ? List.of()
                : permissionRepository.findAllById(request.getPermissions());
        user.setPermissions(new HashSet<>(permissions));

        return userMapper.toUserResponse(userRepository.save(user));
    }

    @PreAuthorize("hasRole('ADMIN')")
    public void deleteUser(String userId) {
        userRepository.deleteById(userId);
    }

    // idea-spec Phase 4 - "11.1 Session Revocation": khoá tài khoản -> chặn ngay token hiện có
    // của user ở AuthenticationService.verifyToken() (gateway gọi mỗi request), đồng thời bắn
    // event "user-events"/USER_LOCKED để chat-service tự ngắt Socket.IO đang mở của user đó
    // (REST bị chặn tức thì qua introspect, nhưng WebSocket không tự re-check per-message nên
    // cần tín hiệu riêng để đóng kết nối thật).
    // idea-spec BA OPS-03: khoá có thời hạn - request bắt buộc reason + duration
    // (THREE_DAYS/SEVEN_DAYS/THIRTY_DAYS/PERMANENT). PERMANENT -> lockedUntil=null, giống hành vi
    // cũ trước khi có OPS-03. UserAutoUnlockJob tự khôi phục khi lockedUntil trôi qua.
    @PreAuthorize("hasRole('ADMIN')")
    @Transactional
    public UserResponse lockUser(String userId, LockUserRequest request) {
        User user = userRepository.findById(userId).orElseThrow(() -> new AppException(ErrorCode.USER_NOT_EXISTED));
        user.setLocked(true);
        user.setLockReason(request.getReason());
        user.setLockedUntil(request.getDuration().computeLockedUntil(Instant.now()));
        userRepository.save(user);

        Map<String, Object> event = new HashMap<>();
        event.put("eventId", UUID.randomUUID().toString());
        event.put("eventType", "USER_LOCKED");
        event.put("aggregateId", user.getId());
        event.put("payload", "{\"userId\":\"" + user.getId() + "\"}");
        event.put("timestamp", System.currentTimeMillis());
        kafkaTemplate.send("user-events", user.getId(), event);

        // idea-spec Phase 7 - "26. Security Hardening / Permission auditing"
        log.info(
                "[AUDIT] admin={} action=LOCK_USER target=user:{} reason={} duration={} lockedUntil={}",
                SecurityContextHolder.getContext().getAuthentication().getName(),
                userId,
                request.getReason(),
                request.getDuration(),
                user.getLockedUntil());

        return userMapper.toUserResponse(user);
    }

    // idea-spec BA GAP-03: thực thi lệnh LOCK_USER từ ADMIN_COMMAND_EXECUTE (report-service) — gọi
    // từ Kafka consumer, không có SecurityContext nên KHÔNG dùng @PreAuthorize + không thể tái
    // dùng lockUser() (đọc Authentication.getName() cho audit log sẽ NPE). Quyền ADMIN đã được
    // report-service xác nhận thật lúc Admin PATCH status (@PreAuthorize ở đó) — đây chỉ thực thi
    // lệnh đã duyệt, không check lại quyền lần 2.
    @Transactional
    public void lockUserBySystem(String userId) {
        userRepository.findById(userId).ifPresent(user -> {
            user.setLocked(true);
            userRepository.save(user);

            Map<String, Object> event = new HashMap<>();
            event.put("eventId", UUID.randomUUID().toString());
            event.put("eventType", "USER_LOCKED");
            event.put("aggregateId", user.getId());
            event.put("payload", "{\"userId\":\"" + user.getId() + "\"}");
            event.put("timestamp", System.currentTimeMillis());
            kafkaTemplate.send("user-events", user.getId(), event);

            log.info("[AUDIT] source=report-service action=LOCK_USER target=user:{}", userId);
        });
    }

    @PreAuthorize("hasRole('ADMIN')")
    @Transactional
    public UserResponse unlockUser(String userId) {
        User user = userRepository.findById(userId).orElseThrow(() -> new AppException(ErrorCode.USER_NOT_EXISTED));
        user.setLocked(false);
        user.setLockedUntil(null);
        user.setLockReason(null);
        userRepository.save(user);
        log.info(
                "[AUDIT] admin={} action=UNLOCK_USER target=user:{}",
                SecurityContextHolder.getContext().getAuthentication().getName(),
                userId);
        return userMapper.toUserResponse(user);
    }

    // idea-spec BA OPS-03: quét mỗi giờ, tự khôi phục các tài khoản khoá có thời hạn đã hết hạn
    // (PERMANENT không nằm trong tập kết quả vì lockedUntil luôn null). Không publish Kafka
    // "user-events" cho auto-unlock - không có consumer nào cần biết (chat-service chỉ cần biết
    // lúc LOCK để ngắt kết nối, REST tự cho qua lại ngay khi introspect() thấy locked=false).
    @Scheduled(fixedRate = 60 * 60 * 1000)
    @Transactional
    public void autoUnlockExpiredUsers() {
        List<User> expired = userRepository.findAllByLockedTrueAndLockedUntilNotNullAndLockedUntilBefore(Instant.now());
        for (User user : expired) {
            user.setLocked(false);
            user.setLockedUntil(null);
            user.setLockReason(null);
            userRepository.save(user);
            log.info("[AUDIT] source=system action=AUTO_UNLOCK_USER target=user:{}", user.getId());
        }
    }

    // idea-spec BA v2 §2.2: "Đặt lại Mật khẩu (Admin Password Reset) - Cấp lại mật khẩu tạm thời
    // cho người dùng khi có yêu cầu hỗ trợ". Không trả mật khẩu thô trong response (tránh lộ qua
    // log/lịch sử request) - chỉ gửi qua email, tái dùng đúng Outbox + WelcomeEmailConsumer có sẵn
    // (consumer đó không rẽ nhánh theo eventType, chỉ đọc subject/body/recipient).
    @PreAuthorize("hasRole('ADMIN')")
    @Transactional
    public void resetPassword(String userId) {
        User user = userRepository.findById(userId).orElseThrow(() -> new AppException(ErrorCode.USER_NOT_EXISTED));

        String tempPassword = generateTempPassword();
        user.setPassword(passwordEncoder.encode(tempPassword));
        userRepository.save(user);

        NotificationEvent notificationEvent = NotificationEvent.builder()
                .channel("EMAIL")
                .userId(user.getId())
                .recipient(user.getEmail())
                .subject("Mật khẩu tạm thời của bạn")
                .body("Mật khẩu tạm thời của bạn là: " + tempPassword
                        + ". Vui lòng đổi mật khẩu ngay sau khi đăng nhập.")
                .build();
        outboxEventService.recordEvent(user.getId(), OutboxEventType.PASSWORD_RESET_EMAIL, notificationEvent);

        log.info(
                "[AUDIT] admin={} action=RESET_PASSWORD target=user:{}",
                SecurityContextHolder.getContext().getAuthentication().getName(),
                userId);
    }

    private static final String TEMP_PASSWORD_CHARS = "ABCDEFGHJKLMNPQRSTUVWXYZabcdefghijkmnopqrstuvwxyz23456789";

    private String generateTempPassword() {
        var random = new java.security.SecureRandom();
        StringBuilder builder = new StringBuilder(12);
        for (int i = 0; i < 12; i++) {
            builder.append(TEMP_PASSWORD_CHARS.charAt(random.nextInt(TEMP_PASSWORD_CHARS.length())));
        }
        return builder.toString();
    }

    // idea-spec BA v2 §2.2: "Xóa/Vô hiệu hóa tài khoản (Soft Delete & Anonymize) - chuyển đổi tài
    // khoản sang trạng thái DEACTIVATED, tự động ẩn profile và chuyển tên tác giả các bài viết
    // công khai thành 'Người dùng đã xóa'". Anonymize thật sự chỉ cần làm ở profile-service (mọi
    // service khác resolve tên tác giả qua đó tại thời điểm đọc, không cache tên riêng).
    @PreAuthorize("hasRole('ADMIN')")
    @Transactional
    public UserResponse deactivateAccount(String userId) {
        User user = userRepository.findById(userId).orElseThrow(() -> new AppException(ErrorCode.USER_NOT_EXISTED));
        user.setDeactivated(true);
        user = userRepository.save(user);

        profileClient.anonymizeProfile(userId);

        // idea-spec BA v2 §2.2: REST đã bị chặn ngay qua verifyToken(), nhưng WebSocket đang mở
        // không tự re-check per-message - cùng tín hiệu "user-events" đã dùng cho LOCK_USER để
        // chat-service tự ngắt kết nối.
        Map<String, Object> event = new HashMap<>();
        event.put("eventId", UUID.randomUUID().toString());
        event.put("eventType", "USER_DEACTIVATED");
        event.put("aggregateId", user.getId());
        event.put("payload", "{\"userId\":\"" + user.getId() + "\"}");
        event.put("timestamp", System.currentTimeMillis());
        kafkaTemplate.send("user-events", user.getId(), event);

        log.info(
                "[AUDIT] admin={} action=DEACTIVATE_ACCOUNT target=user:{}",
                SecurityContextHolder.getContext().getAuthentication().getName(),
                userId);

        return userMapper.toUserResponse(user);
    }

    @PreAuthorize("hasRole('ADMIN')")
    public List<UserResponse> getUsers() {
        log.info("In method get Users");
        return userRepository.findAll().stream().map(userMapper::toUserResponse).toList();
    }

    @PreAuthorize("hasRole('ADMIN')")
    public UserResponse getUser(String id) {
        return userMapper.toUserResponse(
                userRepository.findById(id).orElseThrow(() -> new AppException(ErrorCode.USER_NOT_EXISTED)));
    }
}
