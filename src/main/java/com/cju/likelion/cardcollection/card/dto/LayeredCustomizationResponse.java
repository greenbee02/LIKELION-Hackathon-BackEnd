package com.cju.likelion.cardcollection.card.dto;

import com.fasterxml.jackson.databind.JsonNode;

import java.math.BigDecimal;
import java.time.Instant;
import java.util.List;
import java.util.UUID;

public record LayeredCustomizationResponse(
        UUID id,
        UUID cardId,
        String status,
        List<FrontLayerResponse> frontLayers,
        BackResponse back,
        Instant createdAt
) {
    public record FrontLayerResponse(
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

    public record BackResponse(
            UUID layoutId,
            String baseImageUrl,
            JsonNode layoutData,
            JsonNode contentData
    ) {
    }
}
