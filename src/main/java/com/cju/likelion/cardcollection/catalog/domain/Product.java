package com.cju.likelion.cardcollection.catalog.domain;

import jakarta.persistence.*;
import lombok.AccessLevel;
import lombok.Getter;
import lombok.NoArgsConstructor;

import java.math.BigDecimal;
import java.time.Instant;
import java.util.UUID;

@Entity
@Table(name = "products")
@Getter
@NoArgsConstructor(access = AccessLevel.PROTECTED)
public class Product {

    public static Product of(Brand brand, String name, boolean limited) {
        Product product = new Product();
        product.brand = brand;
        product.name = name;
        product.limited = limited;
        product.active = true;
        return product;
    }

    @Id
    private UUID id;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "brand_id")
    private Brand brand;

    @Column(name = "product_code", length = 100)
    private String productCode;

    @Column(nullable = false, length = 255)
    private String name;

    @Enumerated(EnumType.STRING)
    @Column(name = "offering_type", nullable = false, length = 30)
    private OfferingType offeringType = OfferingType.PRODUCT;

    @Column(length = 100)
    private String category;

    @Column(length = 100)
    private String theme;

    @Column(name = "production_year")
    private Integer productionYear;

    @Column(length = 100)
    private String season;

    @Column(length = 100)
    private String region;

    @Column(length = 100)
    private String material;

    @Column(length = 100)
    private String color;

    @Column(length = 100)
    private String origin;

    @Column(columnDefinition = "TEXT")
    private String description;

    @Column(name = "image_url", length = 1000)
    private String imageUrl;

    @Column(name = "warranty_info", length = 1000)
    private String warrantyInfo;

    @Column(name = "warranty_months")
    private Integer warrantyMonths;

    @Column(columnDefinition = "TEXT")
    private String careInfo;

    @Column(name = "experience_location", length = 500)
    private String experienceLocation;

    private Instant availableFrom;
    private Instant availableUntil;
    private BigDecimal price;

    @Column(name = "is_limited", nullable = false)
    private boolean limited;

    @Column(name = "is_active", nullable = false)
    private boolean active = true;

    @Column(nullable = false)
    private Instant createdAt;

    @Column(nullable = false)
    private Instant updatedAt;

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
