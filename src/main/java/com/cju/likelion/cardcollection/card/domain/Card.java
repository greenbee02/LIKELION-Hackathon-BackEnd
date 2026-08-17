package com.cju.likelion.cardcollection.card.domain;

import com.cju.likelion.cardcollection.auth.domain.User;
import com.cju.likelion.cardcollection.catalog.domain.CardTemplate;
import com.cju.likelion.cardcollection.catalog.domain.Product;
import com.cju.likelion.cardcollection.catalog.domain.Store;
import jakarta.persistence.*;
import lombok.AccessLevel;
import lombok.Getter;
import lombok.NoArgsConstructor;

import java.time.Instant;
import java.util.UUID;

@Entity
@Table(name = "cards")
@Getter
@NoArgsConstructor(access = AccessLevel.PROTECTED)
public class Card {

    @Id
    private UUID id;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "user_id", nullable = false)
    private User user;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "product_id", nullable = false)
    private Product product;

    @OneToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "purchase_qr_id", nullable = false, unique = true)
    private PurchaseQr purchaseQr;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "template_id", nullable = false)
    private CardTemplate template;

    @Enumerated(EnumType.STRING)
    @Column(name = "original_card_type", nullable = false, length = 30)
    private CardType originalCardType;

    @Enumerated(EnumType.STRING)
    @Column(name = "card_type", nullable = false, length = 30)
    private CardType cardType;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 30)
    private CardStatus status;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "selected_customization_id")
    private CardCustomization selectedCustomization;

    @Column(name = "purchase_date", nullable = false)
    private Instant purchaseDate;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "purchase_store_id", nullable = false)
    private Store purchaseStore;

    @Column(name = "serial_number", length = 255)
    private String serialNumber;

    @Column(name = "issued_at", nullable = false)
    private Instant issuedAt;

    @Column(nullable = false)
    private Instant createdAt;

    @Column(nullable = false)
    private Instant updatedAt;

    public static Card issue(
            User user,
            PurchaseQr purchaseQr,
            CardTemplate template,
            Instant now
    ) {
        Card card = new Card();
        card.id = UUID.randomUUID();
        card.user = user;
        card.product = purchaseQr.getProduct();
        card.purchaseQr = purchaseQr;
        card.template = template;
        card.originalCardType = purchaseQr.getProduct().isLimited() ? CardType.COLLECTOR : CardType.BASIC;
        card.cardType = card.originalCardType;
        card.status = CardStatus.ACTIVE;
        card.purchaseDate = purchaseQr.getPurchaseDate();
        card.purchaseStore = purchaseQr.getStore();
        card.serialNumber = purchaseQr.getSerialNumber();
        card.issuedAt = now;
        card.createdAt = now;
        card.updatedAt = now;
        return card;
    }

    public void selectCustomization(CardCustomization customization) {
        selectedCustomization = customization;
        cardType = CardType.CUSTOMIZE;
    }

    public void restoreOriginal() {
        selectedCustomization = null;
        cardType = originalCardType;
    }

    @PrePersist
    void prePersist() {
        if (id == null) id = UUID.randomUUID();
        Instant now = Instant.now();
        if (createdAt == null) createdAt = now;
        if (updatedAt == null) updatedAt = now;
    }

    @PreUpdate
    void preUpdate() {
        updatedAt = Instant.now();
    }
}
