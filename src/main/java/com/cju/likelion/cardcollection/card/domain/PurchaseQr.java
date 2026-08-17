package com.cju.likelion.cardcollection.card.domain;

import com.cju.likelion.cardcollection.auth.domain.User;
import com.cju.likelion.cardcollection.catalog.domain.Product;
import com.cju.likelion.cardcollection.catalog.domain.Store;
import jakarta.persistence.*;
import lombok.AccessLevel;
import lombok.Getter;
import lombok.NoArgsConstructor;

import java.time.Instant;
import java.util.UUID;

@Entity
@Table(name = "purchase_qrs")
@Getter
@NoArgsConstructor(access = AccessLevel.PROTECTED)
public class PurchaseQr {

    public static PurchaseQr of(
            String qrToken,
            Product product,
            Store store,
            Instant purchaseDate,
            Instant expiresAt
    ) {
        PurchaseQr qr = new PurchaseQr();
        qr.qrToken = qrToken;
        qr.product = product;
        qr.store = store;
        qr.purchaseDate = purchaseDate;
        qr.expiresAt = expiresAt;
        return qr;
    }

    @Id
    private UUID id;

    @Column(name = "qr_token", nullable = false, unique = true, length = 255)
    private String qrToken;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "product_id", nullable = false)
    private Product product;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "store_id", nullable = false)
    private Store store;

    @Column(name = "purchase_date", nullable = false)
    private Instant purchaseDate;

    @Column(name = "serial_number", length = 255)
    private String serialNumber;

    @Column(name = "is_used", nullable = false)
    private boolean used;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "used_by")
    private User usedBy;

    @Column(name = "used_at")
    private Instant usedAt;

    @Column(name = "expires_at")
    private Instant expiresAt;

    @Column(nullable = false)
    private Instant createdAt;

    @PrePersist
    void prePersist() {
        if (id == null) id = UUID.randomUUID();
        if (createdAt == null) createdAt = Instant.now();
    }

    public boolean isExpired(Instant now) {
        return expiresAt != null && expiresAt.isBefore(now);
    }

    public void markUsed(User user, Instant now) {
        used = true;
        usedBy = user;
        usedAt = now;
    }
}
