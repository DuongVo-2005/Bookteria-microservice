package com.devteria.identity.service;

import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.when;

import java.time.LocalDate;
import java.util.List;
import java.util.Optional;
import java.util.Set;

import org.assertj.core.api.Assertions;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.kafka.core.KafkaTemplate;
import org.springframework.security.access.AccessDeniedException;
import org.springframework.security.test.context.support.WithMockUser;
import org.springframework.test.context.TestPropertySource;
import org.springframework.test.context.bean.override.mockito.MockitoBean;

import com.devteria.identity.dto.LockDuration;
import com.devteria.identity.dto.request.LockUserRequest;
import com.devteria.identity.dto.request.UserCreationRequest;
import com.devteria.identity.dto.request.UserPermissionUpdateRequest;
import com.devteria.identity.dto.request.UserUpdateRequest;
import com.devteria.identity.dto.response.UserProfileResponse;
import com.devteria.identity.entity.Permission;
import com.devteria.identity.entity.Role;
import com.devteria.identity.entity.User;
import com.devteria.identity.exception.AppException;
import com.devteria.identity.repository.PermissionRepository;
import com.devteria.identity.repository.RoleRepository;
import com.devteria.identity.repository.UserRepository;
import com.devteria.identity.repository.httpclient.ProfileClient;

// Trước đây test file này KHÔNG compile được: UserResponse/User builder gọi
// .firstName()/.lastName()/.dob() nhưng 2 field đó đã bị bỏ khỏi UserResponse/User
// entity từ lâu (chuyển sang profile-service) — chỉ UserCreationRequest còn giữ.
// Viết lại đúng theo contract hiện tại + mock đủ dependency thật của UserService
// (createUser() gọi cả ProfileClient lẫn KafkaTemplate, thiếu mock sẽ lỗi/treo test).
@SpringBootTest
@TestPropertySource("/test.properties")
public class UserServiceTest {
    @Autowired
    private UserService userService;

    @MockitoBean
    private UserRepository userRepository;

    @MockitoBean
    private RoleRepository roleRepository;

    @MockitoBean
    private PermissionRepository permissionRepository;

    @MockitoBean
    private ProfileClient profileClient;

    @MockitoBean
    private KafkaTemplate<String, Object> kafkaTemplate;

    private UserCreationRequest request;
    private User user;

    @BeforeEach
    void initData() {
        LocalDate dob = LocalDate.of(1990, 1, 1);

        request = UserCreationRequest.builder()
                .username("john")
                .firstName("John")
                .lastName("Doe")
                .password("12345678")
                .email("john@test.com")
                .dob(dob)
                .build();

        user = User.builder().id("cf0600f538b3").username("john").build();
    }

    @Test
    void createUser_validRequest_success() {
        when(userRepository.existsByUsername(anyString())).thenReturn(false);
        when(userRepository.save(any())).thenReturn(user);
        when(roleRepository.findById(anyString())).thenReturn(Optional.empty());
        when(profileClient.createProfile(any()))
                .thenReturn(UserProfileResponse.builder().build());

        var response = userService.createUser(request);

        Assertions.assertThat(response.getId()).isEqualTo("cf0600f538b3");
        Assertions.assertThat(response.getUsername()).isEqualTo("john");
    }

    @Test
    void createUser_userExisted_fail() {
        when(userRepository.existsByUsername(anyString())).thenReturn(true);

        var exception = assertThrows(AppException.class, () -> userService.createUser(request));

        Assertions.assertThat(exception.getErrorCode().getCode()).isEqualTo(1002);
    }

    @Test
    // JWT subject là user.getId() (xem UserService.getMyInfo()/AuthenticationService.generateToken),
    // không phải username -> @WithMockUser phải set username = id để mô phỏng đúng SecurityContext.
    @WithMockUser(username = "cf0600f538b3")
    void getMyInfo_valid_success() {
        when(userRepository.findById(anyString())).thenReturn(Optional.of(user));

        var response = userService.getMyInfo();

        Assertions.assertThat(response.getUsername()).isEqualTo("john");
        Assertions.assertThat(response.getId()).isEqualTo("cf0600f538b3");
    }

    @Test
    @WithMockUser(username = "cf0600f538b3")
    void getMyInfo_userNotFound_error() {
        when(userRepository.findById(anyString())).thenReturn(Optional.empty());

        var exception = assertThrows(AppException.class, () -> userService.getMyInfo());

        Assertions.assertThat(exception.getErrorCode().getCode()).isEqualTo(1005);
    }

    // Regression test cho BE-15: xác nhận @PreAuthorize("hasRole('ADMIN')") trên UserService
    // thật sự chặn user không có role ADMIN — đây chính là gate đã suýt bị báo nhầm là "thiếu"
    // khi audit (vì controller không có @PreAuthorize, phải soi xuống service mới thấy).
    @Test
    @WithMockUser(username = "john", roles = "USER")
    void getUsers_withoutAdminRole_accessDenied() {
        assertThrows(AccessDeniedException.class, () -> userService.getUsers());
    }

    @Test
    @WithMockUser(roles = "ADMIN")
    void getUsers_withAdminRole_success() {
        when(userRepository.findAll()).thenReturn(List.of(user));

        var response = userService.getUsers();

        Assertions.assertThat(response).hasSize(1);
        Assertions.assertThat(response.get(0).getUsername()).isEqualTo("john");
    }

    @Test
    @WithMockUser(username = "john", roles = "USER")
    void deleteUser_withoutAdminRole_accessDenied() {
        assertThrows(AccessDeniedException.class, () -> userService.deleteUser("cf0600f538b3"));
    }

    // ---- lockUser()/unlockUser()/autoUnlockExpiredUsers() (OPS-03) ----

    @Test
    @WithMockUser(username = "admin", roles = "USER")
    void lockUser_withoutAdminRole_accessDenied() {
        var request = LockUserRequest.builder()
                .reason("Spam")
                .duration(LockDuration.SEVEN_DAYS)
                .build();

        assertThrows(AccessDeniedException.class, () -> userService.lockUser("cf0600f538b3", request));
    }

    @Test
    @WithMockUser(username = "admin", roles = "ADMIN")
    void lockUser_timedDuration_setsLockedUntilInTheFuture() {
        when(userRepository.findById("cf0600f538b3")).thenReturn(Optional.of(user));
        when(userRepository.save(any())).thenAnswer(inv -> inv.getArgument(0));

        var request = LockUserRequest.builder()
                .reason("Spam")
                .duration(LockDuration.SEVEN_DAYS)
                .build();
        var response = userService.lockUser("cf0600f538b3", request);

        Assertions.assertThat(response.isLocked()).isTrue();
        Assertions.assertThat(response.getLockReason()).isEqualTo("Spam");
        Assertions.assertThat(response.getLockedUntil()).isAfter(java.time.Instant.now());
    }

    @Test
    @WithMockUser(username = "admin", roles = "ADMIN")
    void lockUser_permanentDuration_leavesLockedUntilNull() {
        when(userRepository.findById("cf0600f538b3")).thenReturn(Optional.of(user));
        when(userRepository.save(any())).thenAnswer(inv -> inv.getArgument(0));

        var request = LockUserRequest.builder()
                .reason("Vi phạm nghiêm trọng")
                .duration(LockDuration.PERMANENT)
                .build();
        var response = userService.lockUser("cf0600f538b3", request);

        Assertions.assertThat(response.isLocked()).isTrue();
        Assertions.assertThat(response.getLockedUntil()).isNull();
    }

    @Test
    @WithMockUser(username = "admin", roles = "ADMIN")
    void unlockUser_clearsLockedUntilAndReason() {
        user.setLocked(true);
        user.setLockReason("Spam");
        user.setLockedUntil(java.time.Instant.now().plusSeconds(3600));
        when(userRepository.findById("cf0600f538b3")).thenReturn(Optional.of(user));
        when(userRepository.save(any())).thenAnswer(inv -> inv.getArgument(0));

        var response = userService.unlockUser("cf0600f538b3");

        Assertions.assertThat(response.isLocked()).isFalse();
        Assertions.assertThat(response.getLockedUntil()).isNull();
        Assertions.assertThat(response.getLockReason()).isNull();
    }

    @Test
    void autoUnlockExpiredUsers_unlocksAndClearsFields() {
        User expiredUser = User.builder()
                .id("expired-1")
                .locked(true)
                .lockReason("Spam")
                .lockedUntil(java.time.Instant.now().minusSeconds(60))
                .build();
        when(userRepository.findAllByLockedTrueAndLockedUntilNotNullAndLockedUntilBefore(any()))
                .thenReturn(List.of(expiredUser));
        when(userRepository.save(any())).thenAnswer(inv -> inv.getArgument(0));

        userService.autoUnlockExpiredUsers();

        Assertions.assertThat(expiredUser.isLocked()).isFalse();
        Assertions.assertThat(expiredUser.getLockedUntil()).isNull();
        Assertions.assertThat(expiredUser.getLockReason()).isNull();
    }

    // ---- updatePermissions() (BA v2 P1-01) ----

    @Test
    @WithMockUser(username = "john", roles = "USER")
    void updatePermissions_withoutAdminRole_accessDenied() {
        var request = UserPermissionUpdateRequest.builder()
                .permissions(List.of("book:create"))
                .build();

        assertThrows(AccessDeniedException.class, () -> userService.updatePermissions("cf0600f538b3", request));
    }

    @Test
    @WithMockUser(roles = "ADMIN")
    void updatePermissions_withAdminRole_setsDirectPermissions() {
        when(userRepository.findById("cf0600f538b3")).thenReturn(Optional.of(user));
        Permission bookCreate = Permission.builder().name("book:create").build();
        when(permissionRepository.findAllById(List.of("book:create"))).thenReturn(List.of(bookCreate));
        when(userRepository.save(any())).thenAnswer(inv -> inv.getArgument(0));

        var request = UserPermissionUpdateRequest.builder()
                .permissions(List.of("book:create"))
                .build();
        userService.updatePermissions("cf0600f538b3", request);

        Assertions.assertThat(user.getPermissions()).containsExactly(bookCreate);
    }

    @Test
    @WithMockUser(roles = "ADMIN")
    void updatePermissions_emptyList_clearsDirectPermissions() {
        user.setPermissions(new java.util.HashSet<>(
                Set.of(Permission.builder().name("book:create").build())));
        when(userRepository.findById("cf0600f538b3")).thenReturn(Optional.of(user));
        when(userRepository.save(any())).thenAnswer(inv -> inv.getArgument(0));

        var request =
                UserPermissionUpdateRequest.builder().permissions(List.of()).build();
        userService.updatePermissions("cf0600f538b3", request);

        Assertions.assertThat(user.getPermissions()).isEmpty();
    }

    // ---- resetPassword()/deactivateAccount() (BA v2 §2.2) ----

    @Test
    @WithMockUser(username = "admin", roles = "USER")
    void resetPassword_withoutAdminRole_accessDenied() {
        assertThrows(AccessDeniedException.class, () -> userService.resetPassword("cf0600f538b3"));
    }

    @Test
    @WithMockUser(username = "admin", roles = "ADMIN")
    void resetPassword_withAdminRole_setsNewEncodedPasswordAndPublishesEmail() {
        String oldPasswordHash = user.getPassword();
        when(userRepository.findById("cf0600f538b3")).thenReturn(Optional.of(user));
        when(userRepository.save(any())).thenAnswer(inv -> inv.getArgument(0));

        userService.resetPassword("cf0600f538b3");

        Assertions.assertThat(user.getPassword()).isNotEqualTo(oldPasswordHash);
        Assertions.assertThat(user.getPassword()).isNotNull();
    }

    @Test
    @WithMockUser(username = "admin", roles = "USER")
    void deactivateAccount_withoutAdminRole_accessDenied() {
        assertThrows(AccessDeniedException.class, () -> userService.deactivateAccount("cf0600f538b3"));
    }

    @Test
    @WithMockUser(username = "admin", roles = "ADMIN")
    void deactivateAccount_withAdminRole_setsDeactivatedAndAnonymizesProfile() {
        when(userRepository.findById("cf0600f538b3")).thenReturn(Optional.of(user));
        when(userRepository.save(any())).thenAnswer(inv -> inv.getArgument(0));

        var response = userService.deactivateAccount("cf0600f538b3");

        Assertions.assertThat(response.isDeactivated()).isTrue();
        Assertions.assertThat(user.isDeactivated()).isTrue();
        org.mockito.Mockito.verify(profileClient).anonymizeProfile("cf0600f538b3");
    }

    // ---- updateUser() partial-update regression: bug thật bắt được lúc live-verify BA v2 Phase
    // 1 (2026-09-23) - PUT /users/{userId} chỉ gán role (không kèm password) đã âm thầm null hoá
    // password qua UserMapperImpl.updateUser() (MapStruct set field null vô điều kiện, TRƯỚC cả
    // khi StringUtils.hasText() guard trong UserService kịp chạy) - khoá user khỏi đăng nhập bằng
    // password mà không có lỗi/cảnh báo nào. Test này dùng @SpringBootTest thật (UserMapper KHÔNG
    // mock) để bắt đúng lớp bug này - 1 UserMapper bị mock sẽ không bao giờ lộ ra hành vi generated
    // code thật, đây chính xác là lý do bug tồn tại lâu mà không unit test nào bắt được.
    @Test
    @WithMockUser(roles = "ADMIN")
    void updateUser_roleOnlyNoPassword_keepsExistingPasswordUnchanged() {
        user.setPassword("$2a$10$existingBcryptHash");
        when(userRepository.findById("cf0600f538b3")).thenReturn(Optional.of(user));
        Role librarian = Role.builder().name("LIBRARIAN").build();
        when(roleRepository.findAllById(List.of("LIBRARIAN"))).thenReturn(List.of(librarian));
        when(userRepository.save(any())).thenAnswer(inv -> inv.getArgument(0));

        var request = UserUpdateRequest.builder().roles(List.of("LIBRARIAN")).build();
        userService.updateUser("cf0600f538b3", request);

        Assertions.assertThat(user.getPassword()).isEqualTo("$2a$10$existingBcryptHash");
        Assertions.assertThat(user.getRoles()).containsExactly(librarian);
    }
}
