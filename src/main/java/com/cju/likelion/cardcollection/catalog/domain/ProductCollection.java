package com.cju.likelion.cardcollection.catalog.domain;

import jakarta.persistence.*;
import lombok.AccessLevel;
import lombok.Getter;
import lombok.NoArgsConstructor;

import java.time.Instant;
import java.util.UUID;

@Entity
@Table(name = "product_collections")
@Getter
@NoArgsConstructor(access = AccessLevel.PROTECTED)
public class ProductCollection {

    @Id
    private UUID id;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "brand_id", nullable = false)
    private Brand brand;

    @Column(nullable = false, length = 255)
    private String name;

    @Column(columnDefinition = "TEXT")
    private String description;

    @Column(length = 100)
    private String theme;

    @Column(name = "production_year")
    private Integer productionYear;

    @Column(length = 30)
    private String season;

    @Column(length = 100)
    private String region;

    @Column(name = "is_limited", nullable = false)
    private boolean limited;

    @Column(name = "cover_image_url", length = 1000)
    private String coverImageUrl;

    @Column(nullable = false)
    private Instant createdAt;

    @Column(nullable = false)
    private Instant updatedAt;

    public static ProductCollection of(Brand brand, String name) {
        ProductCollection collection = new ProductCollection();
        collection.id = UUID.randomUUID();
        collection.brand = brand;
        collection.name = name;
        collection.createdAt = Instant.now();
        collection.updatedAt = collection.createdAt;
        return collection;
    }

    @PrePersist
    void prePersist() {
        if (id == null) id = UUID.randomUUID();
        Instant now = Instant.now();
        createdAt = now;
        updatedAt = now;
    }

    @PreUpdate
    void preUpdate() {
        updatedAt = Instant.now();
    }
}
