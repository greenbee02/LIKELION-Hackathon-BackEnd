package com.cju.likelion.cardcollection.auth.controller;

import com.cju.likelion.cardcollection.auth.dto.AuthResponse;
import com.cju.likelion.cardcollection.auth.dto.LoginRequest;
import com.cju.likelion.cardcollection.auth.dto.OAuthCodeExchangeRequest;
import com.cju.likelion.cardcollection.auth.dto.SignupRequest;
import com.cju.likelion.cardcollection.auth.dto.UserResponse;
import com.cju.likelion.cardcollection.auth.service.AuthService;
import com.cju.likelion.cardcollection.auth.service.OAuthLoginCodeStore;
import com.cju.likelion.cardcollection.common.api.ApiResponse;
import jakarta.validation.Valid;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.UUID;

@RestController
@RequestMapping("/api/v1/auth")
public class AuthController {

    private final AuthService authService;
    private final OAuthLoginCodeStore oauthLoginCodeStore;

    public AuthController(AuthService authService, OAuthLoginCodeStore oauthLoginCodeStore) {
        this.authService = authService;
        this.oauthLoginCodeStore = oauthLoginCodeStore;
    }

    @PostMapping("/signup")
    public ResponseEntity<ApiResponse<UserResponse>> signup(
            @Valid @RequestBody SignupRequest request
    ) {
        UserResponse response = authService.signup(request);
        return ResponseEntity.status(HttpStatus.CREATED).body(new ApiResponse<>(response));
    }

    @PostMapping("/login")
    public ResponseEntity<ApiResponse<AuthResponse>> login(
            @Valid @RequestBody LoginRequest request
    ) {
        return ResponseEntity.ok(new ApiResponse<>(authService.login(request)));
    }

    @PostMapping("/oauth/exchange")
    public ResponseEntity<ApiResponse<AuthResponse>> exchangeOAuthCode(
            @Valid @RequestBody OAuthCodeExchangeRequest request
    ) {
        return ResponseEntity.ok(new ApiResponse<>(oauthLoginCodeStore.consume(request.code())));
    }

    @GetMapping("/me")
    public ResponseEntity<ApiResponse<UserResponse>> me(Authentication authentication) {
        UUID userId = UUID.fromString(authentication.getName());
        return ResponseEntity.ok(new ApiResponse<>(authService.getMe(userId)));
    }

    @DeleteMapping("/me")
    public ResponseEntity<Void> withdraw(Authentication authentication) {
        authService.withdraw(UUID.fromString(authentication.getName()));
        return ResponseEntity.noContent().build();
    }
}
