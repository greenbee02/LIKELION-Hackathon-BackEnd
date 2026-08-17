package com.cju.likelion.cardcollection.card.dto;

import com.cju.likelion.cardcollection.card.domain.Card;
import com.cju.likelion.cardcollection.card.domain.CardCustomization;
import com.cju.likelion.cardcollection.catalog.domain.CardTemplate;
import com.cju.likelion.cardcollection.catalog.domain.Product;
import com.cju.likelion.cardcollection.catalog.domain.Store;

import java.time.Instant;
import java.util.UUID;

public record CardResponse(
        UUID id,
        String originalCardType,
        String cardType,
        String status,
        Instant purchaseDate,
        Instant issuedAt,
        String serialNumber,
        ProductSummary product,
        StoreSummary store,
        TemplateSummary template,
        CustomizationSummary selectedCustomization
) {

    public static CardResponse from(Card card) {
        return new CardResponse(
                card.getId(),
                card.getOriginalCardType().name(),
                card.getCardType().name(),
                card.getStatus().name(),
                card.getPurchaseDate(),
                card.getIssuedAt(),
                card.getSerialNumber(),
                ProductSummary.from(card.getProduct()),
                StoreSummary.from(card.getPurchaseStore()),
                TemplateSummary.from(card.getTemplate()),
                CustomizationSummary.from(card.getSelectedCustomization())
        );
    }

    public record ProductSummary(
            UUID id,
            String name,
            String offeringType,
            String category,
            String imageUrl,
            boolean limited
    ) {
        static ProductSummary from(Product product) {
            return new ProductSummary(
                    product.getId(),
                    product.getName(),
                    product.getOfferingType().name(),
                    product.getCategory(),
                    product.getImageUrl(),
                    product.isLimited()
            );
        }
    }

    public record StoreSummary(UUID id, String name, String country, String city) {
        static StoreSummary from(Store store) {
            return new StoreSummary(store.getId(), store.getName(), store.getCountry(), store.getCity());
        }
    }

    public record TemplateSummary(
            UUID id,
            String name,
            String frontImageUrl,
            String backImageUrl,
            String allowedCardType
    ) {
        static TemplateSummary from(CardTemplate template) {
            return new TemplateSummary(
                    template.getId(),
                    template.getName(),
                    template.getFrontImageUrl(),
                    template.getBackImageUrl(),
                    template.getAllowedCardType() == null ? null : template.getAllowedCardType().name()
            );
        }
    }

    public record CustomizationSummary(
            UUID id,
            String status,
            String generatedFrontImageUrl,
            String generatedBackImageUrl,
            String generatedMessage,
            Instant createdAt
    ) {
        static CustomizationSummary from(CardCustomization customization) {
            if (customization == null) return null;
            return new CustomizationSummary(
                    customization.getId(),
                    customization.getGenerationStatus().name(),
                    customization.getGeneratedFrontImageUrl(),
                    customization.getGeneratedBackImageUrl(),
                    customization.getGeneratedMessage(),
                    customization.getCreatedAt()
            );
        }
    }
}
