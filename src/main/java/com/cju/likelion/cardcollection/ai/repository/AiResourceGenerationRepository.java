package com.cju.likelion.cardcollection.ai.repository;

import com.cju.likelion.cardcollection.ai.domain.AiResourceGeneration;
import com.cju.likelion.cardcollection.ai.domain.AiResourceStatus;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.data.jpa.repository.EntityGraph;

import java.time.Instant;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

public interface AiResourceGenerationRepository extends JpaRepository<AiResourceGeneration, UUID> {

    List<AiResourceGeneration> findByCardIdOrderByCreatedAtDesc(UUID cardId);

    Optional<AiResourceGeneration> findByIdAndCardId(UUID resourceId, UUID cardId);

    Optional<AiResourceGeneration> findFirstByGenerationStatusOrderByCreatedAtAsc(AiResourceStatus status);

    @Query(value = """
            SELECT id
            FROM ai_resource_generations
            WHERE generation_status = 'PENDING'
              AND (next_attempt_at IS NULL OR next_attempt_at <= CURRENT_TIMESTAMP)
            ORDER BY created_at ASC
            FOR UPDATE SKIP LOCKED
            LIMIT 1
            """, nativeQuery = true)
    Optional<UUID> findNextPendingId();

    @Transactional
    @Modifying(flushAutomatically = true)
    @Query(value = """
            UPDATE ai_resource_generations
               SET generation_status = 'PROCESSING',
                   processing_started_at = :now,
                   attempt_count = attempt_count + 1,
                   updated_at = :now
             WHERE id = :resourceId
               AND generation_status = 'PENDING'
               AND (next_attempt_at IS NULL OR next_attempt_at <= :now)
            """, nativeQuery = true)
    int claimPending(@Param("resourceId") UUID resourceId, @Param("now") Instant now);

    @Transactional
    @Modifying(flushAutomatically = true)
    @Query(value = """
            UPDATE ai_resource_generations
               SET generation_status = 'PENDING',
                   processing_started_at = NULL,
                   next_attempt_at = CURRENT_TIMESTAMP,
                   failure_reason = LEFT(
                       '작업 점유 시간이 만료되어 재처리합니다. ' || COALESCE(failure_reason, ''),
                       2000
                   ),
                   updated_at = CURRENT_TIMESTAMP
             WHERE generation_status = 'PROCESSING'
               AND processing_started_at < :staleBefore
            """, nativeQuery = true)
    int requeueStaleProcessing(@Param("staleBefore") Instant staleBefore);

    @EntityGraph(attributePaths = {
        "card",
        "card.purchaseStore",
        "product",
        "template"
    })
    @Query("""
            SELECT resource
            FROM AiResourceGeneration resource
            WHERE resource.id = :resourceId
            """)
    Optional<AiResourceGeneration> findForProcessingById(
    @Param("resourceId") UUID resourceId
    );
}
