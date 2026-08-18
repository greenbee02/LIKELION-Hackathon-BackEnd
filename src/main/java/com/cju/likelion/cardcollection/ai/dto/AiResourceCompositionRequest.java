package com.cju.likelion.cardcollection.ai.dto;

import jakarta.validation.constraints.NotEmpty;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;

import java.util.List;
import java.util.Map;
import java.util.UUID;

public record AiResourceCompositionRequest(
        @NotEmpty(message = "resourceIds는 하나 이상 필요합니다.")
        @Size(max = 8, message = "한 번에 최대 8개의 AI 리소스만 조합할 수 있습니다.")
        List<@NotNull(message = "resourceIds의 값은 필수입니다.") UUID> resourceIds,
        @Size(max = 1000, message = "message는 1000자 이하여야 합니다.")
        String message,
        Map<String, Object> layoutData
) {
}
