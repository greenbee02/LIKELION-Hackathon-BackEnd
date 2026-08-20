package com.cju.likelion.cardcollection.card.dto;

import com.cju.likelion.cardcollection.catalog.domain.CardBackLayout;
import com.cju.likelion.cardcollection.catalog.domain.CardDesignAsset;
import com.fasterxml.jackson.databind.JsonNode;

import java.util.List;
import java.util.UUID;

public record CardCustomizationOptionsResponse(
        UUID cardId,
        UUID productId,
        FrontOptionsResponse front,
        BackOptionsResponse back
) {

    public record FrontOptionsResponse(
            List<DesignAssetResponse> productBackgrounds,
            List<DesignAssetResponse> borders
    ) {
    }

    public record DesignAssetResponse(
            UUID id,
            String assetKey,
            String type,
            String name,
            String variantCode,
            String imageUrl,
            boolean transparent,
            int width,
            int height,
            JsonNode metadata
    ) {
        public static DesignAssetResponse from(CardDesignAsset asset, JsonNode metadata) {
            return new DesignAssetResponse(
                    asset.getId(),
                    asset.getAssetKey(),
                    asset.getAssetType().name(),
                    asset.getName(),
                    asset.getVariantCode(),
                    asset.getImageUrl(),
                    asset.isTransparent(),
                    asset.getWidthPx(),
                    asset.getHeightPx(),
                    metadata
            );
        }
    }

    public record BackOptionsResponse(
            UUID layoutId,
            String baseImageUrl,
            JsonNode layoutData
    ) {
        public static BackOptionsResponse from(CardBackLayout layout, JsonNode layoutData) {
            return new BackOptionsResponse(layout.getId(), layout.getBaseAsset().getImageUrl(), layoutData);
        }
    }
}
