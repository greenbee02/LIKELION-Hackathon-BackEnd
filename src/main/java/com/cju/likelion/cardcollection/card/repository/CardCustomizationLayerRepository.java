package com.cju.likelion.cardcollection.card.repository;

import com.cju.likelion.cardcollection.card.domain.CardCustomizationLayer;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;
import java.util.UUID;

public interface CardCustomizationLayerRepository extends JpaRepository<CardCustomizationLayer, UUID> {

    List<CardCustomizationLayer> findByCustomizationIdOrderByLayerOrderAsc(UUID customizationId);
}
