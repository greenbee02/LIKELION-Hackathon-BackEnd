package com.cju.likelion.cardcollection.auth.security;

import com.cju.likelion.cardcollection.auth.dto.AuthResponse;
import com.cju.likelion.cardcollection.auth.exception.OAuthLoginException;
import com.cju.likelion.cardcollection.auth.service.OAuthLoginCodeStore;
import com.cju.likelion.cardcollection.auth.service.SocialLoginService;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.security.core.Authentication;
import org.springframework.security.oauth2.client.authentication.OAuth2AuthenticationToken;
import org.springframework.security.oauth2.core.user.OAuth2User;
import org.springframework.security.web.authentication.AuthenticationSuccessHandler;
import org.springframework.stereotype.Component;
import org.springframework.web.util.UriComponentsBuilder;

import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import java.io.IOException;

@Component
public class OAuth2LoginSuccessHandler implements AuthenticationSuccessHandler {

    private final SocialLoginService socialLoginService;
    private final OAuthLoginCodeStore codeStore;
    private final String frontendUrl;

    public OAuth2LoginSuccessHandler(
            SocialLoginService socialLoginService,
            OAuthLoginCodeStore codeStore,
            @Value("${app.frontend-url:http://localhost:3000}") String frontendUrl
    ) {
        this.socialLoginService = socialLoginService;
        this.codeStore = codeStore;
        this.frontendUrl = frontendUrl;
    }

    @Override
    public void onAuthenticationSuccess(
            HttpServletRequest request,
            HttpServletResponse response,
            Authentication authentication
    ) throws IOException, ServletException {
        try {
            OAuth2AuthenticationToken oauthToken = (OAuth2AuthenticationToken) authentication;
            OAuth2User oauthUser = oauthToken.getPrincipal();
            AuthResponse authResponse = socialLoginService.login(
                    oauthToken.getAuthorizedClientRegistrationId(),
                    oauthUser
            );
            String code = codeStore.issue(authResponse);
            String redirectUrl = UriComponentsBuilder
                    .fromUriString(frontendUrl)
                    .path("/oauth/callback")
                    .queryParam("code", code)
                    .build()
                    .encode()
                    .toUriString();

            response.sendRedirect(redirectUrl);
        } catch (OAuthLoginException exception) {
            response.sendRedirect(frontendUrl + "/oauth/callback?error=OAUTH_LOGIN_FAILED");
        }
    }
}
