package com.cju.likelion.cardcollection.catalog.repository;

import com.cju.likelion.cardcollection.catalog.domain.Store;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.UUID;

public interface StoreRepository extends JpaRepository<Store, UUID> {
}
