package com.cju.likelion.cardcollection.catalog.domain;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.FetchType;
import jakarta.persistence.Id;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.PrePersist;
import jakarta.persistence.PreUpdate;
import jakarta.persistence.Table;
import lombok.AccessLevel;
import lombok.Getter;
import lombok.NoArgsConstructor;
import org.hibernate.annotations.ColumnTransformer;

import java.time.Instant;
import java.util.UUID;

@Entity
@Table(name = "card_design_assets")
@Getter
@NoArgsConstructor(access = AccessLevel.PROTECTED)
public class CardDesignAsset {

    public static CardDesignAsset of(
            Brand brand,
            Product product,
            String assetKey,
            CardDesignAssetType assetType,
            String variantCode,
            String imageUrl,
            boolean transparent,
            String metadata
    ) {
        CardDesignAsset asset = new CardDesignAsset();
        asset.id = UUID.randomUUID();
        asset.brand = brand;
        asset.product = product;
        asset.assetKey = assetKey;
        asset.assetType = assetType;
        asset.name = assetKey;
        asset.variantCode = variantCode;
        asset.imageUrl = imageUrl;
        asset.transparent = transparent;
        asset.widthPx = 1024;
        asset.heightPx = 1536;
        asset.metadata = metadata;
        asset.active = true;
        return asset;
    }

    @Id
    private UUID id;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "brand_id", nullable = false)
    private Brand brand;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "product_id")
    private Product product;

    @Column(name = "asset_key", nullable = false, unique = true, length = 100)
    private String assetKey;

    @Enumerated(EnumType.STRING)
    @Column(name = "asset_type", nullable = false, length = 30)
    private CardDesignAssetType assetType;

    @Column(nullable = false, length = 255)
    private String name;

    @Column(name = "variant_code", nullable = false, length = 30)
    private String variantCode;

    @Column(name = "image_url", nullable = false, length = 1000)
    private String imageUrl;

    @Column(name = "is_transparent", nullable = false)
    private boolean transparent;

    @Column(name = "width_px", nullable = false)
    private int widthPx;

    @Column(name = "height_px", nullable = false)
    private int heightPx;

    @Column(columnDefinition = "jsonb", nullable = false)
    @ColumnTransformer(write = "?::jsonb")
    private String metadata;

    @Column(name = "is_active", nullable = false)
    private boolean active;

    @Column(nullable = false)
    private Instant createdAt;

    @Column(nullable = false)
    private Instant updatedAt;

    @PrePersist
    void prePersist() {
        if (id == null) id = UUID.randomUUID();
        Instant now = Instant.now();
        if (createdAt == null) createdAt = now;
        if (updatedAt == null) updatedAt = now;
    }

    @PreUpdate
    void preUpdate() {
        updatedAt = Instant.now();
    }
}
