package com.cju.likelion.cardcollection.ai.worker;

import com.cju.likelion.cardcollection.ai.domain.AiResourceGeneration;
import com.cju.likelion.cardcollection.ai.domain.AiResourceStatus;
import com.cju.likelion.cardcollection.ai.provider.AiImageProvider;
import com.cju.likelion.cardcollection.ai.provider.AiImageResult;
import com.cju.likelion.cardcollection.ai.provider.AiProviderException;
import com.cju.likelion.cardcollection.ai.repository.AiResourceGenerationRepository;
import com.cju.likelion.cardcollection.ai.storage.GeneratedImageStorage;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

import java.util.UUID;

@Slf4j
@Component
public class AiResourceGenerationWorker {

    private final AiResourceGenerationRepository resourceRepository;
    private final AiImageProvider imageProvider;
    private final GeneratedImageStorage imageStorage;
    private final boolean enabled;
    private final String apiKey;

    public AiResourceGenerationWorker(
            AiResourceGenerationRepository resourceRepository,
            AiImageProvider imageProvider,
            GeneratedImageStorage imageStorage,
            @Value("${app.ai.openai.enabled:false}") boolean enabled,
            @Value("${app.ai.openai.api-key:}") String apiKey
    ) {
        this.resourceRepository = resourceRepository;
        this.imageProvider = imageProvider;
        this.imageStorage = imageStorage;
        this.enabled = enabled;
        this.apiKey = apiKey;
    }

    @Scheduled(fixedDelayString = "${app.ai.worker.fixed-delay-ms:5000}")
    @Transactional
    public void processNext() {
        if (!enabled || apiKey == null || apiKey.isBlank()) return;

        AiResourceGeneration resource = resourceRepository
                .findFirstByGenerationStatusOrderByCreatedAtAsc(AiResourceStatus.PENDING)
                .orElse(null);
        if (resource == null) return;

        process(resource);
    }

    @Transactional
    public void process(UUID resourceId) {
        if (!enabled || apiKey == null || apiKey.isBlank()) return;
        resourceRepository.findById(resourceId).ifPresent(resource -> {
            if (resource.getGenerationStatus() == AiResourceStatus.PENDING) {
                process(resource);
            }
        });
    }

    private void process(AiResourceGeneration resource) {
        long startedAt = System.nanoTime();
        try {
            AiImageResult result = imageProvider.generate(resource);
            String generatedImageUrl = imageStorage.store(
                    resource.getId(), result.imageBytes(), result.contentType());
            resource.complete(generatedImageUrl, result.model());
            log.info("AI resource generation completed: resourceId={}", resource.getId());
        } catch (AiProviderException exception) {
            if (exception.isRejected()) {
                resource.reject(exception.getMessage(), "openai");
            } else {
                resource.fail(exception.getMessage(), "openai");
            }
            log.warn("AI resource generation failed: resourceId={}, status={}",
                    resource.getId(), resource.getGenerationStatus(), exception);
        } catch (RuntimeException exception) {
            resource.fail(exception.getMessage(), "openai");
            log.error("AI resource generation processing failed: resourceId={}", resource.getId(), exception);
        }
        resourceRepository.save(resource);
        long durationMs = (System.nanoTime() - startedAt) / 1_000_000;
        log.info("AI resource generation finished: resourceId={}, status={}, model={}, durationMs={}",
                resource.getId(), resource.getGenerationStatus(), resource.getAiModel(), durationMs);
    }
}
