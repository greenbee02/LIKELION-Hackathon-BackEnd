package com.cju.likelion.cardcollection.ai.dto;

import jakarta.validation.Valid;
import jakarta.validation.constraints.NotEmpty;
import jakarta.validation.constraints.Size;

import java.util.List;

public record AiResourceBatchGenerationRequest(
        @NotEmpty(message = "resources는 하나 이상 필요합니다.")
        @Size(max = 8, message = "AI 리소스 종류는 한 번에 최대 8개까지 선택할 수 있습니다.")
        List<@Valid AiResourceGenerationRequest> resources
) {
}
