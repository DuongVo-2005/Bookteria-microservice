package com.devteria.identity.service;

import java.text.ParseException;
import java.time.Instant;
import java.time.temporal.ChronoUnit;
import java.util.*;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.util.CollectionUtils;

import com.devteria.event.dto.NotificationEvent;
import com.devteria.identity.constant.PredefinedRole;
import com.devteria.identity.dto.OutboxEventType;
import com.devteria.identity.dto.request.*;
import com.devteria.identity.dto.response.AuthenticationResponse;
import com.devteria.identity.dto.response.IntrospectResponse;
import com.devteria.identity.entity.InvalidatedToken;
import com.devteria.identity.entity.OAuthExchangeCode;
import com.devteria.identity.entity.Role;
import com.devteria.identity.entity.User;
import com.devteria.identity.exception.AppException;
import com.devteria.identity.exception.ErrorCode;
import com.devteria.identity.repository.InvalidatedTokenRepository;
import com.devteria.identity.repository.OAuthExchangeCodeRepository;
import com.devteria.identity.repository.RoleRepository;
import com.devteria.identity.repository.UserRepository;
import com.devteria.identity.repository.httpclient.ProfileClient;
import com.nimbusds.jose.*;
import com.nimbusds.jose.crypto.MACSigner;
import com.nimbusds.jose.crypto.MACVerifier;
import com.nimbusds.jwt.JWTClaimsSet;
import com.nimbusds.jwt.SignedJWT;

import lombok.AccessLevel;
import lombok.RequiredArgsConstructor;
import lombok.experimental.FieldDefaults;
import lombok.experimental.NonFinal;
import lombok.extern.slf4j.Slf4j;

@Service
@RequiredArgsConstructor
@Slf4j
@FieldDefaults(level = AccessLevel.PRIVATE, makeFinal = true)
public class AuthenticationService {
    UserRepository userRepository;
    InvalidatedTokenRepository invalidatedTokenRepository;
    OAuthExchangeCodeRepository oAuthExchangeCodeRepository;
    RoleRepository roleRepository;
    ProfileClient profileClient;
    OutboxEventService outboxEventService;

    @NonFinal
    @Value("${jwt.signerKey}")
    protected String signerKey;

    public IntrospectResponse introspect(IntrospectRequest request) {
        var token = request.getToken();
        boolean isValid = true;
        SignedJWT jwt = null;

        try {
            jwt = verifyToken(token);
        } catch (AppException e) {
            isValid = false;
        }

        return IntrospectResponse.builder()
                .userId(Objects.nonNull(jwt) ? claimsOf(jwt).getSubject() : null)
                .valid(isValid)
                .build();
    }

    public AuthenticationResponse authenticate(AuthenticationRequest request) {
        PasswordEncoder passwordEncoder = new BCryptPasswordEncoder(10);
        var user = userRepository
                .findByUsername(request.getUsername())
                .orElseThrow(() -> new AppException(ErrorCode.USER_NOT_EXISTED));

        // Tài khoản tạo qua Google chưa từng đặt password (password == null) —
        // BCryptPasswordEncoder.matches ném IllegalArgumentException nếu so khớp
        // với encodedPassword null, nên phải chặn sớm ở đây.
        if (user.getPassword() == null) {
            throw new AppException(ErrorCode.UNAUTHENTICATED);
        }

        boolean authenticated = passwordEncoder.matches(request.getPassword(), user.getPassword());

        // Bug thật bắt được lúc live-verify: authenticate() trước đây chỉ check password, không
        // check locked/deactivated - verifyToken() (dùng cho MỌI request sau khi đã có token, qua
        // gateway introspect()) có check đúng 2 cờ này, nhưng login lần đầu thì không, nên 1 tài
        // khoản đã bị khoá/deactivate vẫn đăng nhập "thành công" và nhận 1 JWT hợp lệ - chỉ là JWT
        // đó không dùng được ở request tiếp theo. Cùng điều kiện + cùng ErrorCode với verifyToken()
        // (generic UNAUTHENTICATED, không lộ lý do cụ thể cho request chưa xác thực).
        if (user.isLocked() || user.isDeactivated()) {
            throw new AppException(ErrorCode.UNAUTHENTICATED);
        }

        if (!authenticated) throw new AppException(ErrorCode.UNAUTHENTICATED);

        var token = generateToken(user);

        return AuthenticationResponse.builder()
                .token(token.token)
                .expiryTime(token.expiryDate)
                .build();
    }

    public void logout(LogoutRequest request) {
        var signToken = verifyToken(request.getToken());

        String jit = claimsOf(signToken).getJWTID();
        Date expiryTime = claimsOf(signToken).getExpirationTime();

        InvalidatedToken invalidatedToken =
                InvalidatedToken.builder().id(jit).expiryTime(expiryTime).build();

        invalidatedTokenRepository.save(invalidatedToken);
    }

    public AuthenticationResponse refreshToken(RefreshRequest request) {
        var signedJWT = verifyToken(request.getToken());

        var jit = claimsOf(signedJWT).getJWTID();
        var expiryTime = claimsOf(signedJWT).getExpirationTime();

        InvalidatedToken invalidatedToken =
                InvalidatedToken.builder().id(jit).expiryTime(expiryTime).build();

        invalidatedTokenRepository.save(invalidatedToken);

        // JWT subject là user.getId() (xem generateToken), không phải username.
        var userId = claimsOf(signedJWT).getSubject();

        var user = userRepository.findById(userId).orElseThrow(() -> new AppException(ErrorCode.UNAUTHENTICATED));

        var token = generateToken(user);

        return AuthenticationResponse.builder()
                .token(token.token)
                .expiryTime(token.expiryDate)
                .build();
    }

    @Transactional
    public User authenticateGoogleUser(
            String googleSub, String email, boolean emailVerified, String name, String picture) {
        // Case 4: đã link Google trước đó -> nhận diện thẳng bằng sub, không đụng tới email nữa.
        var existingByGoogleSub = userRepository.findByGoogleSub(googleSub);
        if (existingByGoogleSub.isPresent()) {
            return existingByGoogleSub.get();
        }

        // Case 6: email Google chưa verified -> không được dùng để link hay tạo mới,
        // vì không có gì đảm bảo người bấm "Continue with Google" thực sự sở hữu email này.
        if (!emailVerified) {
            throw new AppException(ErrorCode.GOOGLE_EMAIL_NOT_VERIFIED);
        }

        var existingByEmail = userRepository.findByEmail(email);
        User user;
        if (existingByEmail.isPresent()) {
            // Case 1: có account local (hoặc Google account khác) trùng email, đã verified -> link,
            // không tạo User/Profile/Notification mới.
            user = existingByEmail.get();
            user.setGoogleSub(googleSub);
            user.setEmailVerified(true);
            user = userRepository.save(user);
        } else {
            // Case 3: Google user hoàn toàn mới.
            user = createGoogleUser(googleSub, email, name, picture);
        }

        return user;
    }

    public String issueOAuthExchangeCode(User user) {
        String code = UUID.randomUUID().toString();
        Date expiryTime = new Date(Instant.now().plus(60, ChronoUnit.SECONDS).toEpochMilli());

        OAuthExchangeCode exchangeCode = OAuthExchangeCode.builder()
                .code(code)
                .userId(user.getId())
                .expiryTime(expiryTime)
                .build();

        oAuthExchangeCodeRepository.save(exchangeCode);

        return code;
    }

    @Transactional
    public AuthenticationResponse exchangeCode(String code) {
        var exchangeCode = oAuthExchangeCodeRepository
                .findById(code)
                .orElseThrow(() -> new AppException(ErrorCode.INVALID_OAUTH_CODE));

        if (exchangeCode.getExpiryTime().before(new Date())) {
            throw new AppException(ErrorCode.INVALID_OAUTH_CODE);
        }

        oAuthExchangeCodeRepository.delete(exchangeCode);

        var user = userRepository
                .findById(exchangeCode.getUserId())
                .orElseThrow(() -> new AppException(ErrorCode.USER_NOT_EXISTED));

        return buildAuthenticationResponse(user);
    }

    @Scheduled(fixedRate = 300000)
    @Transactional
    public void cleanupExpiredOAuthExchangeCodes() {
        oAuthExchangeCodeRepository.deleteExpired(new Date());
    }

    private User createGoogleUser(String googleSub, String email, String name, String picture) {
        String userName = extractUsernameFromEmail(email);

        // Case 5: username trùng với 1 user khác (email khác) -> không ghi đè, sinh username riêng.
        if (userRepository.existsByUsername(userName)) {
            userName = userName + "_" + System.currentTimeMillis();
        }

        User user = User.builder()
                .username(userName)
                .email(email)
                .googleSub(googleSub)
                .emailVerified(true)
                .build();

        // Gán role USER mặc định cho user đăng ký bằng Google
        HashSet<Role> roles = new HashSet<>();
        roleRepository.findById(PredefinedRole.USER_ROLE).ifPresent(roles::add);
        user.setRoles(roles);

        user = userRepository.save(user);

        // tạo profile
        try {
            ProfileCreationRequest profileCreationRequest = new ProfileCreationRequest();
            profileCreationRequest.setUserId(user.getId());
            profileCreationRequest.setUsername(user.getUsername());
            profileCreationRequest.setFirstName(name);
            profileCreationRequest.setAvatar(picture);
            profileCreationRequest.setEmail(email);

            profileClient.createProfile(profileCreationRequest);
        } catch (Exception e) {
            log.error(
                    "Failed to create profile for Google user: userId={}, username={}",
                    user.getId(),
                    user.getUsername(),
                    e);
        }

        // idea-spec BA GAP-01: Outbox thay vì publish thẳng — xem UserService.createUser() cho lý
        // do đầy đủ.
        NotificationEvent notificationEvent = NotificationEvent.builder()
                .channel("EMAIL")
                .userId(user.getId())
                .recipient(email)
                .subject("Welcome to bookteria")
                .body("Hello, " + user.getUsername())
                .build();
        outboxEventService.recordEvent(user.getId(), OutboxEventType.WELCOME_EMAIL, notificationEvent);

        return user;
    }

    private AuthenticationResponse buildAuthenticationResponse(User user) {
        var token = generateToken(user);

        return AuthenticationResponse.builder()
                .token(token.token())
                .expiryTime(token.expiryDate())
                .build();
    }

    private TokenInfo generateToken(User user) {
        JWSHeader header = new JWSHeader(JWSAlgorithm.HS512);

        Date issueTime = new Date();
        Date expiryTime = new Date(Instant.ofEpochMilli(issueTime.getTime())
                .plus(1, ChronoUnit.HOURS)
                .toEpochMilli());

        JWTClaimsSet jwtClaimsSet = new JWTClaimsSet.Builder()
                .subject(user.getId())
                .issuer("devteria.com")
                .issueTime(issueTime)
                .expirationTime(expiryTime)
                .jwtID(UUID.randomUUID().toString())
                .claim("scope", buildScope(user))
                .claim("userId", user.getId())
                .build();

        Payload payload = new Payload(jwtClaimsSet.toJSONObject());

        JWSObject jwsObject = new JWSObject(header, payload);

        try {
            jwsObject.sign(new MACSigner(signerKey.getBytes()));
            return new TokenInfo(jwsObject.serialize(), expiryTime);
        } catch (JOSEException e) {
            log.error("Cannot create token", e);
            throw new AppException(ErrorCode.UNAUTHENTICATED);
        }
    }

    private SignedJWT verifyToken(String token) {
        // Bọc toàn bộ verify (kể cả SignedJWT.parse() cho token sai cấu trúc, không chỉ sai chữ
        // ký) thành AppException(UNAUTHENTICATED) duy nhất — trước đây ParseException/JOSEException
        // thoát ra ngoài không bị bắt ở introspect()/logout()/refreshToken() (chỉ catch
        // AppException), làm request crash với lỗi 500-class bị Spring Security che thành
        // "Unauthenticated" mù mờ (qua /error bị chặn auth) thay vì trả đúng {valid:false} hoặc
        // lỗi rõ ràng.
        try {
            JWSVerifier verifier = new MACVerifier(signerKey.getBytes());

            SignedJWT signedJWT = SignedJWT.parse(token);

            Date expiryTime = signedJWT.getJWTClaimsSet().getExpirationTime();

            var verified = signedJWT.verify(verifier);

            if (!(verified && expiryTime.after(new Date()))) throw new AppException(ErrorCode.UNAUTHENTICATED);

            if (invalidatedTokenRepository.existsById(
                    signedJWT.getJWTClaimsSet().getJWTID())) throw new AppException(ErrorCode.UNAUTHENTICATED);

            // idea-spec Phase 4 - "11.1 Session Revocation": kiểm tra locked ngay ở đây (không phải
            // chỉ ở introspect()) vì mọi request qua api-gateway đều gọi introspect() -> verifyToken()
            // trên MỌI service (gateway là checkpoint duy nhất, các service khác chỉ tự verify JWT cục
            // bộ không gọi lại introspect) — chặn ở đây tự động "thu hồi session" toàn hệ thống ngay
            // request tiếp theo, không cần biết/enumerate từng JWT đã phát ra của user (khác hẳn cách
            // logout() thu hồi 1 token cụ thể qua InvalidatedToken).
            String userId = signedJWT.getJWTClaimsSet().getSubject();
            userRepository.findById(userId).ifPresent(user -> {
                // idea-spec BA v2 §2.2: tài khoản đã deactivate cũng bị chặn ngay tại đây, cùng
                // cơ chế với locked (thu hồi session toàn hệ thống, không cần biết từng JWT cũ).
                if (user.isLocked() || user.isDeactivated()) {
                    throw new AppException(ErrorCode.UNAUTHENTICATED);
                }
            });

            return signedJWT;
        } catch (ParseException | JOSEException e) {
            throw new AppException(ErrorCode.UNAUTHENTICATED);
        }
    }

    // SignedJWT.getJWTClaimsSet() vẫn khai throws ParseException (checked) dù thực tế không thể
    // ném ở đây nữa — verifyToken() đã tự parse/đọc claims thành công trước khi trả về jwt, lần
    // đọc lại claims (đã cache trong object) không bao giờ fail. Bọc lại để các nơi gọi
    // (introspect/logout/refreshToken) không phải khai throws ParseException nữa — tránh lặp lại
    // đúng lỗi đã sửa ở verifyToken() (checked exception thoát ra ngoài, bị Security che thành
    // "Unauthenticated" mù mờ).
    private JWTClaimsSet claimsOf(SignedJWT jwt) {
        try {
            return jwt.getJWTClaimsSet();
        } catch (ParseException e) {
            throw new AppException(ErrorCode.UNAUTHENTICATED);
        }
    }

    private String buildScope(User user) {
        StringJoiner stringJoiner = new StringJoiner(" ");

        if (!CollectionUtils.isEmpty(user.getRoles()))
            user.getRoles().forEach(role -> {
                stringJoiner.add("ROLE_" + role.getName());
                if (!CollectionUtils.isEmpty(role.getPermissions()))
                    role.getPermissions().forEach(permission -> stringJoiner.add(permission.getName()));
            });

        // idea-spec BA v2 P1-01: permission gán trực tiếp cho user (ngoài permission suy ra từ
        // Role) - downstream service check qua hasAuthority('xxx'); nếu trùng permission đã có từ
        // Role thì scope chỉ có 1 token lặp, vô hại (Spring Security so khớp authority theo tập
        // hợp, không quan tâm trùng lặp).
        if (!CollectionUtils.isEmpty(user.getPermissions()))
            user.getPermissions().forEach(permission -> stringJoiner.add(permission.getName()));

        return stringJoiner.toString();
    }

    private record TokenInfo(String token, Date expiryDate) {}

    private String extractUsernameFromEmail(String email) {
        if (email == null || email.isBlank()) {
            throw new AppException(ErrorCode.UNAUTHENTICATED);
        }
        int atIndex = email.indexOf("@");
        if (atIndex < 0) {
            throw new AppException(ErrorCode.UNAUTHENTICATED);
        }
        return email.substring(0, atIndex);
    }
}
