package com.cju.likelion.cardcollection.catalog.dto;

import com.cju.likelion.cardcollection.catalog.domain.ProductCollection;
import java.util.UUID;

public record ProductCollectionResponse(UUID id, UUID brandId, String brandName, String name, String description,
                                        String theme, Integer productionYear, String season, String region,
                                        boolean limited, String coverImageUrl) {
    public static ProductCollectionResponse from(ProductCollection c) { return new ProductCollectionResponse(c.getId(), c.getBrand().getId(), c.getBrand().getName(), c.getName(), c.getDescription(), c.getTheme(), c.getProductionYear(), c.getSeason(), c.getRegion(), c.isLimited(), c.getCoverImageUrl()); }
}
