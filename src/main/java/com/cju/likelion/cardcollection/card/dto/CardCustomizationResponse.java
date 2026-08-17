package com.cju.likelion.cardcollection.card.dto;

import com.cju.likelion.cardcollection.card.domain.CardCustomization;

import java.time.Instant;
import java.util.UUID;

public record CardCustomizationResponse(
        UUID id,
        UUID cardId,
        UUID templateId,
        String inputImageUrl,
        String inputText,
        String generatedFrontImageUrl,
        String generatedBackImageUrl,
        String generatedMessage,
        String customizationData,
        String aiModel,
        String status,
        Instant createdAt
) {

    public static CardCustomizationResponse from(CardCustomization customization) {
        return new CardCustomizationResponse(
                customization.getId(),
                customization.getCard().getId(),
                customization.getTemplate() == null ? null : customization.getTemplate().getId(),
                customization.getInputImageUrl(),
                customization.getInputText(),
                customization.getGeneratedFrontImageUrl(),
                customization.getGeneratedBackImageUrl(),
                customization.getGeneratedMessage(),
                customization.getCustomizationData(),
                customization.getAiModel(),
                customization.getGenerationStatus().name(),
                customization.getCreatedAt()
        );
    }
}
