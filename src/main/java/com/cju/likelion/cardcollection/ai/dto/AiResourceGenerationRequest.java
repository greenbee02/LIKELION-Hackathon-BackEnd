package com.cju.likelion.cardcollection.ai.dto;

import com.cju.likelion.cardcollection.ai.domain.AiResourceType;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.Size;

import java.util.Map;
import java.util.UUID;

public record AiResourceGenerationRequest(
        @NotNull(message = "resourceType은 필수입니다.")
        AiResourceType resourceType,
        UUID templateId,
        @Size(max = 2000, message = "prompt는 2000자 이하여야 합니다.")
        String prompt,
        Map<String, Object> options,
        @Min(value = 3, message = "candidateCount는 최소 3개입니다.")
        @Max(value = 4, message = "candidateCount는 최대 4개입니다.")
        Integer candidateCount
) {
}
