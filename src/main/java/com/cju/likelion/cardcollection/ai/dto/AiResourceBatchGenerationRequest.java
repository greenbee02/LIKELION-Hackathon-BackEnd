package com.cju.likelion.cardcollection.ai.dto;

import jakarta.validation.Valid;
import jakarta.validation.constraints.NotEmpty;
import jakarta.validation.constraints.Size;

import java.util.List;

public record AiResourceBatchGenerationRequest(
        @NotEmpty(message = "resources는 하나 이상 필요합니다.")
        @Size(min = 3, max = 4, message = "AI 리소스는 한 번에 3~4개를 생성할 수 있습니다.")
        List<@Valid AiResourceGenerationRequest> resources
) {
}
