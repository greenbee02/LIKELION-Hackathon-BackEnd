package com.cju.likelion.cardcollection.catalog.dto;

import com.cju.likelion.cardcollection.catalog.domain.Product;
import java.math.BigDecimal;
import java.time.Instant;
import java.util.UUID;

public record ProductResponse(UUID id, UUID brandId, String brandName, String productCode, String name,
                              String offeringType, String category, String theme, Integer productionYear,
                              String season, String region, String material, String color, String origin,
                              String description, String imageUrl, String warrantyInfo, Integer warrantyMonths,
                              String careInfo, String experienceLocation, Instant availableFrom, Instant availableUntil,
                              BigDecimal price, boolean limited) {
    public static ProductResponse from(Product p) { return new ProductResponse(p.getId(), p.getBrand().getId(), p.getBrand().getName(), p.getProductCode(), p.getName(), p.getOfferingType().name(), p.getCategory(), p.getTheme(), p.getProductionYear(), p.getSeason(), p.getRegion(), p.getMaterial(), p.getColor(), p.getOrigin(), p.getDescription(), p.getImageUrl(), p.getWarrantyInfo(), p.getWarrantyMonths(), p.getCareInfo(), p.getExperienceLocation(), p.getAvailableFrom(), p.getAvailableUntil(), p.getPrice(), p.isLimited()); }
}
