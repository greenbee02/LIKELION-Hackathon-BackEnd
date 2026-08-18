package com.cju.likelion.cardcollection.catalog.repository;

import com.cju.likelion.cardcollection.catalog.domain.ProductCollectionItem;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;
import java.util.UUID;

public interface ProductCollectionItemRepository extends JpaRepository<ProductCollectionItem, UUID> {
    List<ProductCollectionItem> findByProductCollectionId(UUID productCollectionId);
}
