package com.cju.likelion.cardcollection.catalog.dto;
import com.cju.likelion.cardcollection.catalog.domain.CardTemplate;
import java.util.UUID;
public record CardTemplateResponse(UUID id, UUID brandId, String brandName, String name, String description, String frontImageUrl, String backImageUrl, String allowedCardType, String resourceData) { public static CardTemplateResponse from(CardTemplate t) { return new CardTemplateResponse(t.getId(), t.getBrand().getId(), t.getBrand().getName(), t.getName(), t.getDescription(), t.getFrontImageUrl(), t.getBackImageUrl(), t.getAllowedCardType() == null ? null : t.getAllowedCardType().name(), t.getResourceData()); } }
