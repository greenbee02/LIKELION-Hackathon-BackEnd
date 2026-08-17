package com.cju.likelion.cardcollection.catalog.repository;

import com.cju.likelion.cardcollection.catalog.domain.Product;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;
import java.util.UUID;

public interface ProductRepository extends JpaRepository<Product, UUID> {

    List<Product> findByActiveTrueOrderByCreatedAtDesc();
}
