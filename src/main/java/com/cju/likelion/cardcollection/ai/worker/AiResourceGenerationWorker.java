package com.cju.likelion.cardcollection.ai.worker;

import com.cju.likelion.cardcollection.ai.domain.AiResourceGeneration;
import com.cju.likelion.cardcollection.ai.domain.AiResourceStatus;
import com.cju.likelion.cardcollection.ai.provider.AiImageProvider;
import com.cju.likelion.cardcollection.ai.provider.AiImageResult;
import com.cju.likelion.cardcollection.ai.provider.AiProviderException;
import com.cju.likelion.cardcollection.ai.repository.AiResourceGenerationRepository;
import com.cju.likelion.cardcollection.ai.storage.GeneratedImageStorage;
import com.cju.likelion.cardcollection.ai.storage.CardAspectRatioImageNormalizer;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;

import java.time.Duration;
import java.time.Instant;
import java.util.Optional;
import java.util.UUID;

@Slf4j
@Component
public class AiResourceGenerationWorker {

    private final AiResourceGenerationRepository resourceRepository;
    private final AiResourceGenerationClaimService claimService;
    private final AiImageProvider imageProvider;
    private final GeneratedImageStorage imageStorage;
    private final CardAspectRatioImageNormalizer imageNormalizer;
    private final boolean enabled;
    private final String apiKey;

    @Value("${app.ai.worker.max-attempts:3}")
    private int maxAttempts;

    @Value("${app.ai.worker.retry-delay-ms:10000}")
    private long retryDelayMs;

    @Autowired
    public AiResourceGenerationWorker(
            AiResourceGenerationRepository resourceRepository,
            AiResourceGenerationClaimService claimService,
            AiImageProvider imageProvider,
            GeneratedImageStorage imageStorage,
            CardAspectRatioImageNormalizer imageNormalizer,
            @Value("${app.ai.openai.enabled:false}") boolean enabled,
            @Value("${app.ai.openai.api-key:}") String apiKey
    ) {
        this.resourceRepository = resourceRepository;
        this.claimService = claimService;
        this.imageProvider = imageProvider;
        this.imageStorage = imageStorage;
        this.imageNormalizer = imageNormalizer;
        this.enabled = enabled;
        this.apiKey = apiKey;
    }

    public AiResourceGenerationWorker(
            AiResourceGenerationRepository resourceRepository,
            AiImageProvider imageProvider,
            GeneratedImageStorage imageStorage,
            CardAspectRatioImageNormalizer imageNormalizer,
            boolean enabled,
            String apiKey
    ) {
        this(
                resourceRepository,
                new AiResourceGenerationClaimService(resourceRepository, Duration.ofMinutes(15).toMillis()),
                imageProvider,
                imageStorage,
                imageNormalizer,
                enabled,
                apiKey
        );
    }

    @Scheduled(fixedDelayString = "${app.ai.worker.fixed-delay-ms:5000}")
    public void processNext() {
        if (!enabled || apiKey == null || apiKey.isBlank()) return;

        Optional<UUID> resourceId = claimService.claimNext();
        resourceId.ifPresent(this::processClaimed);
    }

    public void process(UUID resourceId) {
        if (!enabled || apiKey == null || apiKey.isBlank()) return;
        if (claimService.claim(resourceId)) processClaimed(resourceId);
    }

    private void processClaimed(UUID resourceId) {
        AiResourceGeneration resource = resourceRepository.findById(resourceId).orElse(null);
        if (resource == null || resource.getGenerationStatus() != AiResourceStatus.PROCESSING) return;

        long startedAt = System.nanoTime();
        try {
            if (resource.getResourceType() == com.cju.likelion.cardcollection.ai.domain.AiResourceType.PRODUCT_ANGLE) {
                resource.reject(
                        "PRODUCT_ANGLE은 신규 AI 생성 대상에서 제외되었습니다.",
                        "system"
                );
                resourceRepository.save(resource);
                logFinished(resource, startedAt);
                return;
            }

            AiImageResult result = imageProvider.generate(resource);
            if (result.hasGeneratedData()) {
                resource.completeData(result.generatedData(), result.model());
                log.info("AI resource data generation completed: resourceId={}", resource.getId());
                resourceRepository.save(resource);
                logFinished(resource, startedAt);
                return;
            }

            result = imageNormalizer.normalize(result);
            String generatedImageUrl = imageStorage.store(
                    resource.getId(), result.imageBytes(), result.contentType());
            resource.complete(generatedImageUrl, result.model());
            log.info("AI resource generation completed: resourceId={}", resource.getId());
        } catch (AiProviderException exception) {
            if (exception.isRejected()) {
                resource.reject(exception.getMessage(), "openai");
            } else {
                scheduleRetryOrFail(resource, exception.getMessage(), "openai");
            }
            log.warn("AI resource generation failed: resourceId={}, status={}",
                    resource.getId(), resource.getGenerationStatus(), exception);
        } catch (RuntimeException exception) {
            scheduleRetryOrFail(resource, exception.getMessage(), "openai");
            log.error("AI resource generation processing failed: resourceId={}", resource.getId(), exception);
        }
        resourceRepository.save(resource);
        logFinished(resource, startedAt);
    }

    private void scheduleRetryOrFail(AiResourceGeneration resource, String reason, String model) {
        long exponent = Math.max(0, resource.getAttemptCount() - 1);
        long multiplier = 1L << Math.min(exponent, 5);
        long delay = Math.min(retryDelayMs * multiplier, Duration.ofMinutes(5).toMillis());
        boolean retried = resource.retryOrFail(
                reason,
                model,
                maxAttempts,
                Instant.now().plusMillis(delay)
        );
        if (retried) {
            log.info("AI resource generation scheduled for retry: resourceId={}, attempt={}, nextAttemptAt={}",
                    resource.getId(), resource.getAttemptCount(), resource.getNextAttemptAt());
        }
    }

    private void logFinished(AiResourceGeneration resource, long startedAt) {
        long durationMs = (System.nanoTime() - startedAt) / 1_000_000;
        log.info("AI resource generation finished: resourceId={}, status={}, model={}, durationMs={}",
                resource.getId(), resource.getGenerationStatus(), resource.getAiModel(), durationMs);
    }
}
