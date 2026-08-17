package com.cju.likelion.cardcollection.catalog.repository;

import com.cju.likelion.cardcollection.catalog.domain.CardTemplate;
import com.cju.likelion.cardcollection.card.domain.CardType;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;
import java.util.UUID;

public interface CardTemplateRepository extends JpaRepository<CardTemplate, UUID> {

    List<CardTemplate> findAllByActiveTrueOrderByCreatedAtAsc();

    List<CardTemplate> findByActiveTrueAndAllowedCardTypeIsNullOrActiveTrueAndAllowedCardType(
            CardType cardType
    );
}
