package com.cju.likelion.cardcollection.auth.dto;

public record AuthResponse(String accessToken, UserResponse user, long expiresInSeconds) {
}
