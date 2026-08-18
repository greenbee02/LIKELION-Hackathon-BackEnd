package com.cju.likelion.cardcollection.ai.repository;

import com.cju.likelion.cardcollection.ai.domain.AiResourceGeneration;
import com.cju.likelion.cardcollection.ai.domain.AiResourceStatus;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

public interface AiResourceGenerationRepository extends JpaRepository<AiResourceGeneration, UUID> {

    List<AiResourceGeneration> findByCardIdOrderByCreatedAtDesc(UUID cardId);

    Optional<AiResourceGeneration> findByIdAndCardId(UUID resourceId, UUID cardId);

    Optional<AiResourceGeneration> findFirstByGenerationStatusOrderByCreatedAtAsc(AiResourceStatus status);
}
