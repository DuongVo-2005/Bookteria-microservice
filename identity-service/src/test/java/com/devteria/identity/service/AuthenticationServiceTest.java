package com.devteria.identity.service;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.when;

import java.util.Optional;

import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;
import org.springframework.test.context.TestPropertySource;
import org.springframework.test.context.bean.override.mockito.MockitoBean;

import com.devteria.identity.dto.request.AuthenticationRequest;
import com.devteria.identity.entity.User;
import com.devteria.identity.exception.AppException;
import com.devteria.identity.exception.ErrorCode;
import com.devteria.identity.repository.UserRepository;

// idea-spec BA v2 §2.2 follow-up: bug thật bắt được lúc live-verify - authenticate() (login lần
// đầu) trước đây chỉ check password, KHÔNG check locked/deactivated (khác verifyToken() dùng cho
// mọi request SAU khi đã có token, đã check đúng 2 cờ này từ Phase 4 §11.1). Kết quả: 1 tài khoản
// đã bị khoá/deactivate vẫn "đăng nhập thành công" và nhận JWT hợp lệ - chỉ vô dụng ở request kế
// tiếp. AuthenticationService trước đây 0 test.
@SpringBootTest
@TestPropertySource("/test.properties")
class AuthenticationServiceTest {

    private static final String RAW_PASSWORD = "correct-password";

    @Autowired
    private AuthenticationService authenticationService;

    @MockitoBean
    private UserRepository userRepository;

    // UserService.autoUnlockExpiredUsers() là @Scheduled(fixedRate=...) - Spring tự chạy ngay 1
    // lần lúc context này khởi động (fixedRate không có initialDelay). Vì test này có bộ
    // @MockitoBean khác UserServiceTest nên Spring tạo 1 context RIÊNG (không tái dùng context
    // cache), khiến job đó chạy đúng lúc race với chính test method, đụng vào userRepository mock
    // đang giữa lúc when()-stubbing ở thread khác -> Mockito lỗi WrongTypeOfReturnValue mù mờ,
    // không liên quan gì logic authenticate() đang test. Mock hẳn UserService (bean chứa job đó)
    // để Spring không bao giờ đăng ký job này trong context test - cách chặn triệt để, không phải
    // đoán timing.
    @MockitoBean
    private UserService userService;

    private User userWithPassword(boolean locked, boolean deactivated) {
        String hash = new BCryptPasswordEncoder(10).encode(RAW_PASSWORD);
        return User.builder()
                .id("user-1")
                .username("john")
                .password(hash)
                .locked(locked)
                .deactivated(deactivated)
                .build();
    }

    private AuthenticationRequest request() {
        return AuthenticationRequest.builder()
                .username("john")
                .password(RAW_PASSWORD)
                .build();
    }

    @Test
    void authenticate_activeAccountCorrectPassword_returnsToken() {
        when(userRepository.findByUsername("john")).thenReturn(Optional.of(userWithPassword(false, false)));

        var response = authenticationService.authenticate(request());

        assertNotNull(response.getToken());
    }

    @Test
    void authenticate_lockedAccountCorrectPassword_rejected() {
        when(userRepository.findByUsername("john")).thenReturn(Optional.of(userWithPassword(true, false)));

        var ex = assertThrows(AppException.class, () -> authenticationService.authenticate(request()));

        assertEquals(ErrorCode.UNAUTHENTICATED, ex.getErrorCode());
    }

    @Test
    void authenticate_deactivatedAccountCorrectPassword_rejected() {
        when(userRepository.findByUsername("john")).thenReturn(Optional.of(userWithPassword(false, true)));

        var ex = assertThrows(AppException.class, () -> authenticationService.authenticate(request()));

        assertEquals(ErrorCode.UNAUTHENTICATED, ex.getErrorCode());
    }
}
