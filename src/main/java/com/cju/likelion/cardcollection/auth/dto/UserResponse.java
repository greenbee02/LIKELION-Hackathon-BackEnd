package com.cju.likelion.cardcollection.auth.dto;

import com.cju.likelion.cardcollection.auth.domain.User;
import com.cju.likelion.cardcollection.auth.domain.UserRole;

import java.util.UUID;

public record UserResponse(UUID id, String email, String name, UserRole role) {

    public static UserResponse from(User user) {
        return new UserResponse(user.getId(), user.getEmail(), user.getName(), user.getRole());
    }
}
