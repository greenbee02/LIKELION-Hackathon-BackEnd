package com.cju.likelion.cardcollection.ai.dto;

import java.util.List;
import java.util.UUID;

public record AiResourceGenerationBatchResponse(
        UUID cardId,
        List<AiResourceCandidateGroupResponse> groups
) {
}
