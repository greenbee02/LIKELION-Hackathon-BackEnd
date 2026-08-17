package com.cju.likelion.cardcollection.card.repository;

import com.cju.likelion.cardcollection.card.domain.PurchaseQr;
import jakarta.persistence.LockModeType;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Lock;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.util.Optional;
import java.util.UUID;

public interface PurchaseQrRepository extends JpaRepository<PurchaseQr, UUID> {

    @Lock(LockModeType.PESSIMISTIC_WRITE)
    @Query("""
            select q from PurchaseQr q
            join fetch q.product
            join fetch q.store
            where q.qrToken = :qrToken
            """)
    Optional<PurchaseQr> findByQrTokenForUpdate(@Param("qrToken") String qrToken);
}
