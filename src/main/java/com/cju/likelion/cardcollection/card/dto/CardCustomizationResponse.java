package com.cju.likelion.cardcollection.card.dto;

import com.cju.likelion.cardcollection.card.domain.CardCustomization;
import com.cju.likelion.cardcollection.card.domain.CardCustomizationLayer;
import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;

import java.math.BigDecimal;
import java.time.Instant;
import java.util.List;
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
        List<FrontLayerSummary> frontLayers,
        BackSummary back,
        Instant createdAt
) {

    private static final ObjectMapper OBJECT_MAPPER = new ObjectMapper();

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
                customization.getLayers().stream().map(CardCustomizationResponse::toLayerSummary).toList(),
                customization.getBackLayout() == null ? null : new BackSummary(
                        customization.getBackLayout().getId(),
                        customization.getBackLayout().getBaseAsset().getImageUrl(),
                        readJson(customization.getBackLayout().getLayoutData()),
                        readJson(customization.getBackContentData())
                ),
                customization.getCreatedAt()
        );
    }

    private static FrontLayerSummary toLayerSummary(CardCustomizationLayer layer) {
        return new FrontLayerSummary(
                layer.getLayerType().name(),
                layer.getAsset() == null ? null : layer.getAsset().getId(),
                layer.getAsset() == null ? null : layer.getAsset().getImageUrl(),
                layer.getTextContent(),
                layer.getLayerOrder(),
                layer.getPositionX(),
                layer.getPositionY(),
                layer.getWidth(),
                layer.getHeight(),
                layer.getRotation(),
                layer.getOpacity(),
                layer.getZIndex(),
                readJson(layer.getStyleData())
        );
    }

    private static JsonNode readJson(String value) {
        try {
            return OBJECT_MAPPER.readTree(value == null || value.isBlank() ? "{}" : value);
        } catch (JsonProcessingException exception) {
            throw new IllegalStateException("저장된 커스터마이징 JSON 데이터가 올바르지 않습니다.", exception);
        }
    }

    public record FrontLayerSummary(
            String type,
            UUID assetId,
            String imageUrl,
            String textContent,
            int layerOrder,
            BigDecimal x,
            BigDecimal y,
            BigDecimal width,
            BigDecimal height,
            BigDecimal rotation,
            BigDecimal opacity,
            int zIndex,
            JsonNode styleData
    ) {
    }

    public record BackSummary(
            UUID layoutId,
            String baseImageUrl,
            JsonNode layoutData,
            JsonNode contentData
    ) {
    }
}
