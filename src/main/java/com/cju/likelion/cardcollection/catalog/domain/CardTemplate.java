package com.cju.likelion.cardcollection.catalog.domain;

import com.cju.likelion.cardcollection.card.domain.CardType;
import jakarta.persistence.*;
import lombok.AccessLevel;
import lombok.Getter;
import lombok.NoArgsConstructor;

import java.time.Instant;
import java.util.UUID;

@Entity
@Table(name = "card_templates")
@Getter
@NoArgsConstructor(access = AccessLevel.PROTECTED)
public class CardTemplate {

    public static CardTemplate of(
            Brand brand,
            String name,
            String frontImageUrl,
            String backImageUrl,
            CardType allowedCardType
    ) {
        CardTemplate template = new CardTemplate();
        template.brand = brand;
        template.name = name;
        template.frontImageUrl = frontImageUrl;
        template.backImageUrl = backImageUrl;
        template.allowedCardType = allowedCardType;
        template.active = true;
        return template;
    }

    @Id
    private UUID id;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "brand_id", nullable = false)
    private Brand brand;

    @Column(nullable = false, length = 255)
    private String name;

    @Column(columnDefinition = "TEXT")
    private String description;

    @Column(name = "front_image_url", nullable = false, length = 1000)
    private String frontImageUrl;

    @Column(name = "back_image_url", nullable = false, length = 1000)
    private String backImageUrl;

    @Enumerated(EnumType.STRING)
    @Column(name = "allowed_card_type", length = 30)
    private CardType allowedCardType;

    @Column(name = "resource_data", columnDefinition = "TEXT")
    private String resourceData;

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
