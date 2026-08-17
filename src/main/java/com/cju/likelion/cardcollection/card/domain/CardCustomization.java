package com.cju.likelion.cardcollection.card.domain;

import com.cju.likelion.cardcollection.catalog.domain.CardTemplate;
import jakarta.persistence.*;
import lombok.AccessLevel;
import lombok.Getter;
import lombok.NoArgsConstructor;

import java.time.Instant;
import java.util.UUID;

@Entity
@Table(name = "card_customizations")
@Getter
@NoArgsConstructor(access = AccessLevel.PROTECTED)
public class CardCustomization {

    @Id
    private UUID id;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "card_id", nullable = false)
    private Card card;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "template_id")
    private CardTemplate template;

    @Column(name = "input_image_url", length = 1000)
    private String inputImageUrl;

    @Column(name = "input_text", length = 1000)
    private String inputText;

    @Column(name = "generated_front_image_url", length = 1000)
    private String generatedFrontImageUrl;

    @Column(name = "generated_back_image_url", length = 1000)
    private String generatedBackImageUrl;

    @Column(name = "generated_message", length = 1000)
    private String generatedMessage;

    @Column(name = "customization_data", columnDefinition = "TEXT")
    private String customizationData;

    @Column(name = "ai_model", length = 100)
    private String aiModel;

    @Enumerated(EnumType.STRING)
    @Column(name = "generation_status", nullable = false, length = 30)
    private CustomizationStatus generationStatus;

    @Column(nullable = false)
    private Instant createdAt;

    @Column(nullable = false)
    private Instant updatedAt;

    public static CardCustomization completed(
            Card card,
            CardTemplate template,
            String inputImageUrl,
            String inputText,
            String generatedMessage,
            Instant now
    ) {
        CardCustomization customization = new CardCustomization();
        customization.id = UUID.randomUUID();
        customization.card = card;
        customization.template = template;
        customization.inputImageUrl = inputImageUrl;
        customization.inputText = inputText;
        customization.generatedFrontImageUrl = template.getFrontImageUrl();
        customization.generatedBackImageUrl = template.getBackImageUrl();
        customization.generatedMessage = generatedMessage;
        customization.customizationData = "{\"source\":\"mock\"}";
        customization.aiModel = "mock-v1";
        customization.generationStatus = CustomizationStatus.COMPLETED;
        customization.createdAt = now;
        customization.updatedAt = now;
        return customization;
    }

    public boolean isCompleted() {
        return generationStatus == CustomizationStatus.COMPLETED;
    }

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
