package com.cju.likelion.cardcollection.catalog.repository;

import com.cju.likelion.cardcollection.catalog.domain.CardBackLayout;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;
import java.util.UUID;

public interface CardBackLayoutRepository extends JpaRepository<CardBackLayout, UUID> {

    List<CardBackLayout> findByBrandIdAndActiveTrueOrderByCreatedAtAsc(UUID brandId);
}
