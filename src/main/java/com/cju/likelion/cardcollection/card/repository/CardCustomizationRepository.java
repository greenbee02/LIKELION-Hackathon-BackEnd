package com.cju.likelion.cardcollection.card.repository;

import com.cju.likelion.cardcollection.card.domain.CardCustomization;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

public interface CardCustomizationRepository extends JpaRepository<CardCustomization, UUID> {

    List<CardCustomization> findByCardIdOrderByCreatedAtDesc(UUID cardId);

    Optional<CardCustomization> findByIdAndCardId(UUID customizationId, UUID cardId);
}
