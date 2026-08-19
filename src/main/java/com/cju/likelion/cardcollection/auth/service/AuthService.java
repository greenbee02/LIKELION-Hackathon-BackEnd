package com.cju.likelion.cardcollection.auth.service;

import com.cju.likelion.cardcollection.auth.domain.User;
import com.cju.likelion.cardcollection.auth.domain.UserRole;
import com.cju.likelion.cardcollection.auth.dto.AuthResponse;
import com.cju.likelion.cardcollection.auth.dto.LoginRequest;
import com.cju.likelion.cardcollection.auth.dto.SignupRequest;
import com.cju.likelion.cardcollection.auth.dto.UserResponse;
import com.cju.likelion.cardcollection.auth.exception.DuplicateEmailException;
import com.cju.likelion.cardcollection.auth.exception.InvalidCredentialsException;
import com.cju.likelion.cardcollection.auth.repository.UserRepository;
import com.cju.likelion.cardcollection.auth.security.JwtTokenProvider;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.Locale;
import java.util.UUID;

@Service
public class AuthService {

    private final UserRepository userRepository;
    private final PasswordEncoder passwordEncoder;
    private final JwtTokenProvider jwtTokenProvider;

    public AuthService(
            UserRepository userRepository,
            PasswordEncoder passwordEncoder,
            JwtTokenProvider jwtTokenProvider
    ) {
        this.userRepository = userRepository;
        this.passwordEncoder = passwordEncoder;
        this.jwtTokenProvider = jwtTokenProvider;
    }

    @Transactional
    public UserResponse signup(SignupRequest request) {
        String email = normalizeEmail(request.email());
        if (userRepository.existsByEmail(email)) {
            throw new DuplicateEmailException();
        }

        User user = User.builder()
                .email(email)
                .passwordHash(passwordEncoder.encode(request.password()))
                .name(request.name().trim())
                .role(UserRole.CUSTOMER)
                .build();

        return UserResponse.from(userRepository.save(user));
    }

    @Transactional(readOnly = true)
    public AuthResponse login(LoginRequest request) {
        String email = normalizeEmail(request.email());
        User user = userRepository.findByEmailAndDeletedAtIsNull(email)
                .orElseThrow(InvalidCredentialsException::new);

        if (!passwordEncoder.matches(request.password(), user.getPasswordHash())) {
            throw new InvalidCredentialsException();
        }

        String accessToken = jwtTokenProvider.createToken(user.getId(), user.getRole());
        return new AuthResponse(
                accessToken,
                UserResponse.from(user),
                jwtTokenProvider.getExpirationSeconds()
        );
    }

    @Transactional(readOnly = true)
    public UserResponse getMe(UUID userId) {
        return userRepository.findByIdAndDeletedAtIsNull(userId)
                .map(UserResponse::from)
                .orElseThrow(InvalidCredentialsException::new);
    }

    @Transactional
    public void withdraw(UUID userId) {
        User user = userRepository.findByIdAndDeletedAtIsNull(userId)
                .orElseThrow(InvalidCredentialsException::new);
        user.withdraw();
    }

    private String normalizeEmail(String email) {
        return email.trim().toLowerCase(Locale.ROOT);
    }
}
