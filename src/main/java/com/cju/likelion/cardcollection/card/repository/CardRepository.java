package com.cju.likelion.cardcollection.card.repository;

import com.cju.likelion.cardcollection.card.domain.Card;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;
import java.util.Optional;
import java.util.UUID;
import com.cju.likelion.cardcollection.card.domain.CardStatus;

public interface CardRepository extends JpaRepository<Card, UUID> {

    List<Card> findByUserIdOrderByCreatedAtDesc(UUID userId);

    Optional<Card> findByIdAndUserId(UUID cardId, UUID userId);

    List<Card> findByUserIdAndStatus(UUID userId, CardStatus status);
}
