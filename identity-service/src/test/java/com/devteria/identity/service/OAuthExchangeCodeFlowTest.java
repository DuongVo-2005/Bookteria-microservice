package com.devteria.identity.service;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.when;

import java.util.Date;
import java.util.Optional;

import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.TestPropertySource;
import org.springframework.test.context.bean.override.mockito.MockitoBean;

import com.devteria.identity.entity.User;
import com.devteria.identity.exception.AppException;
import com.devteria.identity.exception.ErrorCode;
import com.devteria.identity.repository.OAuthExchangeCodeRepository;
import com.devteria.identity.repository.UserRepository;

// Regression test cho Google login Phase 2 (exchange code): xác nhận round-trip
// issue -> exchange thật qua JPA/H2 cho OAuthExchangeCodeRepository (không mock,
// để bắt đúng loại bug timezone/serialization mà test tay qua mysql-cli đã từng gây
// hiểu nhầm). UserRepository vẫn mock như UserServiceTest — bảng "user" là từ khoá
// dự trữ trong H2 nên chưa từng được test tay chạm tới thật, ngoài phạm vi task này.
@SpringBootTest
@TestPropertySource(locations = "/test.properties", properties = "spring.jpa.hibernate.ddl-auto=update")
class OAuthExchangeCodeFlowTest {

    @Autowired
    private AuthenticationService authenticationService;

    @MockitoBean
    private UserRepository userRepository;

    @Autowired
    private OAuthExchangeCodeRepository oAuthExchangeCodeRepository;

    private User testUser(String id) {
        User user = User.builder().id(id).username("oauth_test_" + id).build();
        when(userRepository.findById(id)).thenReturn(Optional.of(user));
        return user;
    }

    @Test
    void issueThenExchange_returnsValidToken() {
        User user = testUser("user-1");

        String code = authenticationService.issueOAuthExchangeCode(user);
        var response = authenticationService.exchangeCode(code);

        assertNotNull(response.getToken());
    }

    @Test
    void exchangeSameCodeTwice_secondCallRejected() {
        User user = testUser("user-2");

        String code = authenticationService.issueOAuthExchangeCode(user);
        authenticationService.exchangeCode(code);

        AppException ex = assertThrows(AppException.class, () -> authenticationService.exchangeCode(code));
        assertEquals(ErrorCode.INVALID_OAUTH_CODE, ex.getErrorCode());
    }

    @Test
    void exchangeExpiredCode_rejected() {
        User user = testUser("user-3");

        String code = authenticationService.issueOAuthExchangeCode(user);

        // Force hết hạn: đọc lại bản ghi vừa issue, chỉnh expiryTime về quá khứ rồi save lại.
        var exchangeCode = oAuthExchangeCodeRepository.findById(code).orElseThrow();
        exchangeCode.setExpiryTime(new Date(System.currentTimeMillis() - 1000));
        oAuthExchangeCodeRepository.save(exchangeCode);

        AppException ex = assertThrows(AppException.class, () -> authenticationService.exchangeCode(code));
        assertEquals(ErrorCode.INVALID_OAUTH_CODE, ex.getErrorCode());
    }

    @Test
    void exchangeUnknownCode_rejected() {
        AppException ex = assertThrows(AppException.class, () -> authenticationService.exchangeCode("does-not-exist"));
        assertEquals(ErrorCode.INVALID_OAUTH_CODE, ex.getErrorCode());
    }
}
