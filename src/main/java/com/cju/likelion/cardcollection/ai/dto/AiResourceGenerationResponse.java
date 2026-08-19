package com.cju.likelion.cardcollection.ai.dto;

import com.cju.likelion.cardcollection.ai.domain.AiResourceGeneration;

import java.time.Instant;
import java.util.UUID;

public record AiResourceGenerationResponse(
        UUID id,
        UUID candidateGroupId,
        Integer candidateIndex,
        Integer candidateCount,
        UUID cardId,
        UUID productId,
        UUID templateId,
        String resourceType,
        String prompt,
        String sourceImageUrl,
        String generatedImageUrl,
        String generatedData,
        String aiModel,
        String status,
        String failureReason,
        Instant createdAt,
        Instant updatedAt
) {

    public static AiResourceGenerationResponse from(AiResourceGeneration resource) {
        return new AiResourceGenerationResponse(
                resource.getId(),
                resource.getCandidateGroupId(),
                resource.getCandidateIndex(),
                resource.getCandidateCount(),
                resource.getCard().getId(),
                resource.getProduct() == null ? null : resource.getProduct().getId(),
                resource.getTemplate() == null ? null : resource.getTemplate().getId(),
                resource.getResourceType().name(),
                resource.getPrompt(),
                resource.getSourceImageUrl(),
                resource.getGeneratedImageUrl(),
                resource.getGeneratedData(),
                resource.getAiModel(),
                resource.getGenerationStatus().name(),
                resource.getFailureReason(),
                resource.getCreatedAt(),
                resource.getUpdatedAt()
        );
    }
}
