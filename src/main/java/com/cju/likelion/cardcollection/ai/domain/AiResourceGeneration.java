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

    @Column(name = "candidate_group_id")
    private UUID candidateGroupId;

    @Column(name = "candidate_index")
    private Integer candidateIndex;

    @Column(name = "candidate_count")
    private Integer candidateCount;

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

    /** 요청 시점의 옵션을 보존한다. 구조화된 리소스는 생성 완료 후 generatedData가 결과로 바뀌기 때문이다. */
    @Column(name = "options_data", columnDefinition = "TEXT")
    private String optionsData;

    @Column(name = "ai_model", length = 100)
    private String aiModel;

    @Enumerated(EnumType.STRING)
    @Column(name = "generation_status", nullable = false, length = 30)
    private AiResourceStatus generationStatus;

    @Column(name = "failure_reason", length = 2000)
    private String failureReason;

    @Column(name = "processing_started_at")
    private Instant processingStartedAt;

    @Column(name = "attempt_count", nullable = false)
    private int attemptCount;

    @Column(name = "next_attempt_at")
    private Instant nextAttemptAt;

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
            UUID candidateGroupId,
            Integer candidateIndex,
            Integer candidateCount,
            Instant now
    ) {
        this.id = UUID.randomUUID();
        this.candidateGroupId = candidateGroupId;
        this.candidateIndex = candidateIndex;
        this.candidateCount = candidateCount;
        this.card = card;
        this.product = product;
        this.template = template;
        this.resourceType = resourceType;
        this.prompt = prompt;
        this.sourceImageUrl = sourceImageUrl;
        this.generatedData = generatedData;
        this.optionsData = generatedData;
        this.generationStatus = AiResourceStatus.PENDING;
        this.attemptCount = 0;
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
            UUID candidateGroupId,
            Integer candidateIndex,
            Integer candidateCount,
            Instant now
    ) {
        return new AiResourceGeneration(
                card, product, template, resourceType, prompt, sourceImageUrl, generatedData,
                candidateGroupId, candidateIndex, candidateCount, now);
    }

    public void complete(String generatedImageUrl, String aiModel) {
        this.generatedImageUrl = generatedImageUrl;
        this.aiModel = aiModel;
        this.generationStatus = AiResourceStatus.COMPLETED;
        this.failureReason = null;
        clearProcessingState();
    }

    public void completeData(String generatedData, String aiModel) {
        this.generatedImageUrl = null;
        this.generatedData = generatedData;
        this.aiModel = aiModel;
        this.generationStatus = AiResourceStatus.COMPLETED;
        this.failureReason = null;
        clearProcessingState();
    }

    public boolean retryOrFail(
            String failureReason,
            String aiModel,
            int maxAttempts,
            Instant nextAttemptAt
    ) {
        this.aiModel = aiModel;
        this.failureReason = truncate(failureReason);
        clearProcessingState();
        if (attemptCount < maxAttempts) {
            this.generationStatus = AiResourceStatus.PENDING;
            this.nextAttemptAt = nextAttemptAt;
            return true;
        }
        this.generationStatus = AiResourceStatus.FAILED;
        this.nextAttemptAt = null;
        return false;
    }

    public void reject(String failureReason, String aiModel) {
        this.aiModel = aiModel;
        this.failureReason = truncate(failureReason);
        this.generationStatus = AiResourceStatus.REJECTED;
        clearProcessingState();
    }

    private void clearProcessingState() {
        this.processingStartedAt = null;
        this.nextAttemptAt = null;
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
