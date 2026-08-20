package com.cju.likelion.cardcollection.card.domain;

import com.cju.likelion.cardcollection.catalog.domain.CardDesignAsset;
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

import java.math.BigDecimal;
import java.time.Instant;
import java.util.UUID;

@Entity
@Table(name = "card_customization_layers")
@Getter
@NoArgsConstructor(access = AccessLevel.PROTECTED)
public class CardCustomizationLayer {

    public static CardCustomizationLayer assetLayer(
            CardCustomization customization,
            CardDesignAsset asset,
            CardCustomizationLayerType layerType,
            int layerOrder,
            int zIndex
    ) {
        CardCustomizationLayer layer = base(customization, layerType, layerOrder, zIndex);
        layer.asset = asset;
        return layer;
    }

    public static CardCustomizationLayer textLayer(
            CardCustomization customization,
            String textContent,
            BigDecimal positionX,
            BigDecimal positionY,
            BigDecimal width,
            BigDecimal height,
            BigDecimal rotation,
            BigDecimal opacity,
            int zIndex,
            String styleData
    ) {
        CardCustomizationLayer layer = base(customization, CardCustomizationLayerType.TEXT, 2, zIndex);
        layer.textContent = textContent;
        layer.positionX = positionX;
        layer.positionY = positionY;
        layer.width = width;
        layer.height = height;
        layer.rotation = rotation;
        layer.opacity = opacity;
        layer.styleData = styleData;
        return layer;
    }

    private static CardCustomizationLayer base(
            CardCustomization customization,
            CardCustomizationLayerType layerType,
            int layerOrder,
            int zIndex
    ) {
        CardCustomizationLayer layer = new CardCustomizationLayer();
        layer.id = UUID.randomUUID();
        layer.customization = customization;
        layer.layerType = layerType;
        layer.layerOrder = layerOrder;
        layer.positionX = BigDecimal.ZERO;
        layer.positionY = BigDecimal.ZERO;
        layer.width = BigDecimal.ONE;
        layer.height = BigDecimal.ONE;
        layer.rotation = BigDecimal.ZERO;
        layer.opacity = BigDecimal.ONE;
        layer.zIndex = zIndex;
        layer.styleData = "{}";
        return layer;
    }

    @Id
    private UUID id;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "customization_id", nullable = false)
    private CardCustomization customization;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "asset_id")
    private CardDesignAsset asset;

    @Enumerated(EnumType.STRING)
    @Column(name = "layer_type", nullable = false, length = 30)
    private CardCustomizationLayerType layerType;

    @Column(name = "layer_order", nullable = false)
    private int layerOrder;

    @Column(name = "text_content", length = 1000)
    private String textContent;

    @Column(name = "position_x", nullable = false, precision = 8, scale = 6)
    private BigDecimal positionX;

    @Column(name = "position_y", nullable = false, precision = 8, scale = 6)
    private BigDecimal positionY;

    @Column(nullable = false, precision = 8, scale = 6)
    private BigDecimal width;

    @Column(nullable = false, precision = 8, scale = 6)
    private BigDecimal height;

    @Column(nullable = false, precision = 8, scale = 3)
    private BigDecimal rotation;

    @Column(nullable = false, precision = 7, scale = 6)
    private BigDecimal opacity;

    @Column(name = "z_index", nullable = false)
    private int zIndex;

    @Column(name = "style_data", columnDefinition = "jsonb", nullable = false)
    @ColumnTransformer(write = "?::jsonb")
    private String styleData;

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
