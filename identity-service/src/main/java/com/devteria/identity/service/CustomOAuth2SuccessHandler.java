package com.devteria.identity.service;

import java.io.IOException;

import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.security.core.Authentication;
import org.springframework.security.oauth2.core.user.OAuth2User;
import org.springframework.security.web.authentication.SimpleUrlAuthenticationSuccessHandler;
import org.springframework.stereotype.Component;
import org.springframework.web.util.UriComponentsBuilder;

import com.devteria.identity.entity.User;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;

@Component
@Slf4j
@RequiredArgsConstructor
public class CustomOAuth2SuccessHandler extends SimpleUrlAuthenticationSuccessHandler {

    final AuthenticationService authenticationService;

    @Value("${app.oauth2.redirect-uri}")
    String oauth2RedirectUri;

    @Override
    public void onAuthenticationSuccess(
            HttpServletRequest request, HttpServletResponse response, Authentication authentication)
            throws IOException, ServletException {
        // Lấy thông tin user gg gửi về
        OAuth2User oAuth2User = (OAuth2User) authentication.getPrincipal();
        String email = oAuth2User.getAttribute("email");
        String name = oAuth2User.getAttribute("name");
        String picture = oAuth2User.getAttribute("picture");
        String sub = oAuth2User.getAttribute("sub");
        Boolean emailVerified = oAuth2User.getAttribute("email_verified");

        User user = authenticationService.authenticateGoogleUser(
                sub, email, Boolean.TRUE.equals(emailVerified), name, picture);
        String code = authenticationService.issueOAuthExchangeCode(user);

        // Xác định URL đích ở FE để chuyển hướng rediret — chỉ mang code ngắn hạn,
        // FE phải POST /auth/oauth2/exchange để đổi lấy JWT thật, tránh JWT lộ qua URL/log.
        String targetUrl = UriComponentsBuilder.fromUriString(oauth2RedirectUri)
                .queryParam("code", code)
                .build()
                .toUriString();

        // Điều hướng Client
        if (response.isCommitted()) {
            logger.debug("Response has already been committed");
            return;
        }

        getRedirectStrategy().sendRedirect(request, response, targetUrl);
    }
}
