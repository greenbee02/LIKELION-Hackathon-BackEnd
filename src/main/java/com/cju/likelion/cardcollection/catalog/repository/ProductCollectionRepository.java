package com.cju.likelion.cardcollection.catalog.repository;

import com.cju.likelion.cardcollection.catalog.domain.ProductCollection;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.UUID;

public interface ProductCollectionRepository extends JpaRepository<ProductCollection, UUID> {
}
