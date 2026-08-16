package com.cju.likelion.cardcollection.auth.service;

import com.cju.likelion.cardcollection.auth.domain.SocialAccount;
import com.cju.likelion.cardcollection.auth.domain.SocialProvider;
import com.cju.likelion.cardcollection.auth.domain.User;
import com.cju.likelion.cardcollection.auth.domain.UserRole;
import com.cju.likelion.cardcollection.auth.dto.AuthResponse;
import com.cju.likelion.cardcollection.auth.dto.UserResponse;
import com.cju.likelion.cardcollection.auth.exception.OAuthLoginException;
import com.cju.likelion.cardcollection.auth.repository.SocialAccountRepository;
import com.cju.likelion.cardcollection.auth.repository.UserRepository;
import com.cju.likelion.cardcollection.auth.security.JwtTokenProvider;
import org.springframework.security.oauth2.core.user.OAuth2User;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.Locale;
import java.util.Map;
import java.util.Optional;

@Service
public class SocialLoginService {

    private final UserRepository userRepository;
    private final SocialAccountRepository socialAccountRepository;
    private final JwtTokenProvider jwtTokenProvider;

    public SocialLoginService(
            UserRepository userRepository,
            SocialAccountRepository socialAccountRepository,
            JwtTokenProvider jwtTokenProvider
    ) {
        this.userRepository = userRepository;
        this.socialAccountRepository = socialAccountRepository;
        this.jwtTokenProvider = jwtTokenProvider;
    }

    @Transactional
    public AuthResponse login(String registrationId, OAuth2User oauthUser) {
        SocialProvider provider = parseProvider(registrationId);
        SocialProfile profile = extractProfile(provider, oauthUser.getAttributes());

        Optional<SocialAccount> socialAccount = socialAccountRepository
                .findByProviderAndProviderUserId(provider, profile.providerUserId());
        User user;
        if (socialAccount.isPresent()) {
            user = socialAccount.get().getUser();
            if (user.isWithdrawn()) {
                throw new OAuthLoginException("탈퇴한 계정은 다시 로그인할 수 없습니다.");
            }
        } else {
            user = findOrCreateUser(provider, profile);
        }

        String accessToken = jwtTokenProvider.createToken(user.getId(), user.getRole());
        return new AuthResponse(
                accessToken,
                UserResponse.from(user),
                jwtTokenProvider.getExpirationSeconds()
        );
    }

    private User findOrCreateUser(SocialProvider provider, SocialProfile profile) {
        User user = userRepository.findByEmail(profile.email()).orElse(null);
        if (user != null && user.isWithdrawn()) {
            throw new OAuthLoginException("탈퇴한 계정은 다시 로그인할 수 없습니다.");
        }
        if (user == null) {
            user = userRepository.save(User.builder()
                    .email(profile.email())
                    .name(profile.name())
                    .role(UserRole.CUSTOMER)
                    .build());
        }

        socialAccountRepository.save(SocialAccount.builder()
                .user(user)
                .provider(provider)
                .providerUserId(profile.providerUserId())
                .build());

        return user;
    }

    private SocialProvider parseProvider(String registrationId) {
        try {
            return SocialProvider.valueOf(registrationId.toUpperCase(Locale.ROOT));
        } catch (IllegalArgumentException exception) {
            throw new OAuthLoginException("지원하지 않는 소셜 로그인 제공자입니다.");
        }
    }

    private SocialProfile extractProfile(SocialProvider provider, Map<String, Object> attributes) {
        return switch (provider) {
            case GOOGLE -> extractGoogleProfile(attributes);
            case KAKAO -> extractKakaoProfile(attributes);
        };
    }

    private SocialProfile extractGoogleProfile(Map<String, Object> attributes) {
        String providerUserId = requiredValue(attributes.get("sub"), "Google 사용자 식별자");
        String email = requiredEmail(attributes.get("email"), "Google 이메일");
        if (!Boolean.TRUE.equals(attributes.get("email_verified"))) {
            throw new OAuthLoginException("Google 이메일 인증이 확인되지 않았습니다.");
        }

        String name = optionalValue(attributes.get("name"), "Google 사용자");
        return new SocialProfile(providerUserId, email, name);
    }

    private SocialProfile extractKakaoProfile(Map<String, Object> attributes) {
        String providerUserId = requiredValue(attributes.get("id"), "카카오 사용자 식별자");
        Map<String, Object> account = mapValue(attributes.get("kakao_account"));
        Map<String, Object> profile = mapValue(account.get("profile"));
        String email = requiredEmail(account.get("email"), "카카오 이메일");

        boolean emailValid = Boolean.TRUE.equals(account.get("is_email_valid"));
        boolean emailVerified = Boolean.TRUE.equals(account.get("is_email_verified"));
        if (!emailValid || !emailVerified) {
            throw new OAuthLoginException("카카오 이메일 인증이 확인되지 않았습니다.");
        }

        String name = optionalValue(profile.get("nickname"), "카카오 사용자");
        return new SocialProfile(providerUserId, email, name);
    }

    @SuppressWarnings("unchecked")
    private Map<String, Object> mapValue(Object value) {
        if (value instanceof Map<?, ?> map) {
            return (Map<String, Object>) map;
        }
        throw new OAuthLoginException("소셜 사용자 정보 형식이 올바르지 않습니다.");
    }

    private String requiredEmail(Object value, String label) {
        return requiredValue(value, label).toLowerCase(Locale.ROOT);
    }

    private String requiredValue(Object value, String label) {
        if (value == null || value.toString().isBlank()) {
            throw new OAuthLoginException(label + "을(를) 제공받지 못했습니다.");
        }
        return value.toString();
    }

    private String optionalValue(Object value, String defaultValue) {
        return value == null || value.toString().isBlank() ? defaultValue : value.toString();
    }

    private record SocialProfile(String providerUserId, String email, String name) {
    }
}
