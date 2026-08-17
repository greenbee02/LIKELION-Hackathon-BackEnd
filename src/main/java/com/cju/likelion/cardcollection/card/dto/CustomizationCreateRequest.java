package com.cju.likelion.cardcollection.card.dto;

import jakarta.validation.constraints.Size;

import java.util.UUID;

public record CustomizationCreateRequest(
        UUID templateId,
        @Size(max = 1000, message = "inputImageUrl은 1000자 이하여야 합니다.")
        String inputImageUrl,
        @Size(max = 1000, message = "inputText는 1000자 이하여야 합니다.")
        String inputText
) {
}
