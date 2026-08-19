package com.cju.likelion.cardcollection.ai.domain;

import com.cju.likelion.cardcollection.card.domain.Card;
import com.cju.likelion.cardcollection.catalog.domain.CardTemplate;
import com.cju.likelion.cardcollection.catalog.domain.Product;
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

import java.time.Instant;
import java.util.UUID;

@Entity
@Table(name = "ai_resource_generations")
@Getter
@NoArgsConstructor(access = AccessLevel.PROTECTED)
public class AiResourceGeneration {

    @Id
    private UUID id;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "card_id", nullable = false)
    private Card card;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "product_id")
    private Product product;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "template_id")
    private CardTemplate template;

    @Enumerated(EnumType.STRING)
    @Column(name = "resource_type", nullable = false, length = 30)
    private AiResourceType resourceType;

    @Column(length = 2000)
    private String prompt;

    @Column(name = "source_image_url", length = 1000)
    private String sourceImageUrl;

    @Column(name = "generated_image_url", length = 1000)
    private String generatedImageUrl;

    @Column(name = "generated_data", columnDefinition = "TEXT")
    private String generatedData;

    @Column(name = "ai_model", length = 100)
    private String aiModel;

    @Enumerated(EnumType.STRING)
    @Column(name = "generation_status", nullable = false, length = 30)
    private AiResourceStatus generationStatus;

    @Column(name = "failure_reason", length = 2000)
    private String failureReason;

    @Column(nullable = false)
    private Instant createdAt;

    @Column(nullable = false)
    private Instant updatedAt;

    private AiResourceGeneration(
            Card card,
            Product product,
            CardTemplate template,
            AiResourceType resourceType,
            String prompt,
            String sourceImageUrl,
            String generatedData,
            Instant now
    ) {
        this.id = UUID.randomUUID();
        this.card = card;
        this.product = product;
        this.template = template;
        this.resourceType = resourceType;
        this.prompt = prompt;
        this.sourceImageUrl = sourceImageUrl;
        this.generatedData = generatedData;
        this.generationStatus = AiResourceStatus.PENDING;
        this.createdAt = now;
        this.updatedAt = now;
    }

    public static AiResourceGeneration pending(
            Card card,
            Product product,
            CardTemplate template,
            AiResourceType resourceType,
            String prompt,
            String sourceImageUrl,
            String generatedData,
            Instant now
    ) {
        return new AiResourceGeneration(
                card, product, template, resourceType, prompt, sourceImageUrl, generatedData, now);
    }

    public void complete(String generatedImageUrl, String aiModel) {
        this.generatedImageUrl = generatedImageUrl;
        this.aiModel = aiModel;
        this.generationStatus = AiResourceStatus.COMPLETED;
        this.failureReason = null;
    }

    public void completeData(String generatedData, String aiModel) {
        this.generatedImageUrl = null;
        this.generatedData = generatedData;
        this.aiModel = aiModel;
        this.generationStatus = AiResourceStatus.COMPLETED;
        this.failureReason = null;
    }

    public void fail(String failureReason, String aiModel) {
        this.aiModel = aiModel;
        this.failureReason = truncate(failureReason);
        this.generationStatus = AiResourceStatus.FAILED;
    }

    public void reject(String failureReason, String aiModel) {
        this.aiModel = aiModel;
        this.failureReason = truncate(failureReason);
        this.generationStatus = AiResourceStatus.REJECTED;
    }

    private String truncate(String value) {
        if (value == null) return null;
        return value.length() <= 2000 ? value : value.substring(0, 2000);
    }

    @PrePersist
    void prePersist() {
        if (id == null) id = UUID.randomUUID();
        Instant now = Instant.now();
        if (createdAt == null) createdAt = now;
        if (updatedAt == null) updatedAt = now;
        if (generationStatus == null) generationStatus = AiResourceStatus.PENDING;
    }

    @PreUpdate
    void preUpdate() {
        updatedAt = Instant.now();
    }
}
