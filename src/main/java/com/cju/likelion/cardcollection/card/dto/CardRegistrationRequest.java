package com.cju.likelion.cardcollection.card.dto;

import jakarta.validation.constraints.NotBlank;

public record CardRegistrationRequest(
        @NotBlank(message = "qrToken은 필수입니다.")
        String qrToken
) {
}
