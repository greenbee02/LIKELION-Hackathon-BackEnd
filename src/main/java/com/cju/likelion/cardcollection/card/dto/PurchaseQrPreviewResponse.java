package com.cju.likelion.cardcollection.card.dto;

import com.cju.likelion.cardcollection.card.domain.PurchaseQr;
import com.cju.likelion.cardcollection.catalog.domain.Product;
import com.cju.likelion.cardcollection.catalog.domain.Store;

import java.time.Instant;
import java.util.UUID;

public record PurchaseQrPreviewResponse(
        String status,
        boolean usable,
        Instant purchaseDate,
        String serialNumber,
        Instant expiresAt,
        ProductSummary product,
        StoreSummary store
) {

    public static PurchaseQrPreviewResponse from(PurchaseQr qr, Instant now) {
        String status = qr.isUsed() ? "USED" : qr.isExpired(now) ? "EXPIRED" : "AVAILABLE";
        return new PurchaseQrPreviewResponse(
                status,
                "AVAILABLE".equals(status),
                qr.getPurchaseDate(),
                qr.getSerialNumber(),
                qr.getExpiresAt(),
                ProductSummary.from(qr.getProduct()),
                StoreSummary.from(qr.getStore())
        );
    }

    public record ProductSummary(
            UUID id,
            String productCode,
            String name,
            String imageUrl,
            boolean limited
    ) {
        static ProductSummary from(Product product) {
            return new ProductSummary(
                    product.getId(),
                    product.getProductCode(),
                    product.getName(),
                    product.getImageUrl(),
                    product.isLimited()
            );
        }
    }

    public record StoreSummary(
            UUID id,
            String name,
            String country,
            String city
    ) {
        static StoreSummary from(Store store) {
            return new StoreSummary(store.getId(), store.getName(), store.getCountry(), store.getCity());
        }
    }
}
