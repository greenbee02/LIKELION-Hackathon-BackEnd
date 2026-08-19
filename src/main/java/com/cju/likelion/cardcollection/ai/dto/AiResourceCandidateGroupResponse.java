package com.cju.likelion.cardcollection.ai.dto;

import java.util.List;
import java.util.UUID;

public record AiResourceCandidateGroupResponse(
        UUID candidateGroupId,
        String resourceType,
        int candidateCount,
        List<AiResourceGenerationResponse> candidates
) {
}
