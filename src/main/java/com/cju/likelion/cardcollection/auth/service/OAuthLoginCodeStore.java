package com.cju.likelion.cardcollection.auth.service;

import com.cju.likelion.cardcollection.auth.dto.AuthResponse;
import com.cju.likelion.cardcollection.auth.exception.OAuthLoginException;
import org.springframework.stereotype.Component;

import java.time.Duration;
import java.time.Instant;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;

@Component
public class OAuthLoginCodeStore {

    private static final Duration CODE_TTL = Duration.ofMinutes(2);

    private final ConcurrentHashMap<String, StoredAuth> store = new ConcurrentHashMap<>();

    public String issue(AuthResponse authResponse) {
        String code = UUID.randomUUID().toString();
        store.put(code, new StoredAuth(authResponse, Instant.now().plus(CODE_TTL)));
        return code;
    }

    public AuthResponse consume(String code) {
        StoredAuth storedAuth = store.remove(code);
        if (storedAuth == null || storedAuth.expiresAt().isBefore(Instant.now())) {
            throw new OAuthLoginException("유효하지 않거나 만료된 로그인 코드입니다.");
        }
        return storedAuth.authResponse();
    }

    private record StoredAuth(AuthResponse authResponse, Instant expiresAt) {
    }
}
