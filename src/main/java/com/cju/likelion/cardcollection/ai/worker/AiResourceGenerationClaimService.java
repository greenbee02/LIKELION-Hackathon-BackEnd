package com.cju.likelion.cardcollection.ai.worker;

import com.cju.likelion.cardcollection.ai.repository.AiResourceGenerationRepository;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.Instant;
import java.util.Optional;
import java.util.UUID;

@Service
public class AiResourceGenerationClaimService {

    private final AiResourceGenerationRepository resourceRepository;
    private final long staleTimeoutMs;

    public AiResourceGenerationClaimService(
            AiResourceGenerationRepository resourceRepository,
            @Value("${app.ai.worker.stale-timeout-ms:900000}") long staleTimeoutMs
    ) {
        this.resourceRepository = resourceRepository;
        this.staleTimeoutMs = staleTimeoutMs;
    }

    @Transactional
    public Optional<UUID> claimNext() {
        requeueStaleProcessing();
        Optional<UUID> resourceId = resourceRepository.findNextPendingId();
        if (resourceId.isEmpty()) return Optional.empty();

        Instant now = Instant.now();
        return resourceRepository.claimPending(resourceId.get(), now) == 1
                ? resourceId
                : Optional.empty();
    }

    @Transactional
    public boolean claim(UUID resourceId) {
        requeueStaleProcessing();
        return resourceRepository.claimPending(resourceId, Instant.now()) == 1;
    }

    private void requeueStaleProcessing() {
        resourceRepository.requeueStaleProcessing(
                Instant.now().minusMillis(staleTimeoutMs));
    }
}
